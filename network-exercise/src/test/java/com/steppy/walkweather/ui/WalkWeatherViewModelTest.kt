package com.steppy.walkweather.ui

import com.steppy.walkweather.data.CachedForecast
import com.steppy.walkweather.data.ForecastCache
import com.steppy.walkweather.data.ForecastLoad
import com.steppy.walkweather.data.OpenMeteoApi
import com.steppy.walkweather.data.WeatherRepository
import com.steppy.walkweather.domain.City
import com.steppy.walkweather.domain.Forecast
import com.steppy.walkweather.domain.HourForecast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * The repository and ViewModel together, with real HTTP against a MockWebServer,
 * so each failure goes through the same code as on a phone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WalkWeatherViewModelTest {

    private class MemoryCache(var saved: CachedForecast? = null) : ForecastCache {
        override suspend fun load() = saved
        override suspend fun save(forecast: CachedForecast) { saved = forecast }
    }

    private val dispatcher = StandardTestDispatcher()
    private lateinit var server: MockWebServer
    private val cache = MemoryCache()
    private var clock = 1_000_000_000L

    private val berlin = City(2950159, "Berlin", 52.52, 13.41, "State of Berlin", "Germany")
    private val lisbon = City(2267057, "Lisbon", 38.72, -9.13, "Lisbon", "Portugal")

    private val forecastBody = """
        {"hourly":{"time":["2026-11-26T14:00","2026-11-26T15:00"],
                   "temperature_2m":[14.0,15.0],"precipitation_probability":[10,5],
                   "weather_code":[1,0],"wind_speed_10m":[8.0,9.0]}}
    """.trimIndent()

    private fun savedForecast(city: City, fetchedAt: Long) = CachedForecast(
        Forecast(city, listOf(HourForecast(LocalDateTime.of(2026, 11, 26, 9, 0), 12.0, 0, 0, 5.0))),
        fetchedAt
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        server.shutdown()
    }

    private fun viewModel(): WalkWeatherViewModel {
        val base = server.url("/")
        val api = OpenMeteoApi(OkHttpClient(), base, base, io = dispatcher)
        return WalkWeatherViewModel(WeatherRepository(api, cache) { clock }) { clock }
    }

    private fun enqueue(body: String, code: Int = 200) =
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))

    private fun TestScope.settle() = advanceUntilIdle()

    // ---- search states

    @Test
    fun search_goesThroughLoadingToResults() = runTest(dispatcher) {
        enqueue("""{"results":[{"id":2950159,"name":"Berlin","latitude":52.52,"longitude":13.41}]}""")
        val vm = viewModel()

        vm.search("  Berlin ")
        assertEquals(SearchState.Loading("Berlin"), vm.search.value)
        settle()
        assertEquals(1, (vm.search.value as SearchState.Results).cities.size)
    }

    @Test
    fun search_withNoMatches_isEmpty() = runTest(dispatcher) {
        enqueue("""{"generationtime_ms":0.1}""")
        val vm = viewModel()
        vm.search("zzqqxx")
        settle()
        assertEquals(SearchState.Empty("zzqqxx"), vm.search.value)
    }

    @Test
    fun search_failure_thenRetry_works() = runTest(dispatcher) {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        enqueue("""{"results":[{"id":1,"name":"Berlin","latitude":52.52,"longitude":13.41}]}""")
        val vm = viewModel()

        vm.search("Berlin")
        settle()
        val error = vm.search.value as SearchState.Error
        assertTrue(error.message.startsWith("No internet connection"))

        vm.retrySearch()
        settle()
        assertTrue(vm.search.value is SearchState.Results)
    }

    @Test
    fun tooShortQuery_sendsNothing() = runTest(dispatcher) {
        val vm = viewModel()
        vm.search("B")
        settle()
        assertEquals(SearchState.Idle, vm.search.value)
        assertEquals(0, server.requestCount)
    }

    // ---- forecast states

    @Test
    fun pickingACity_loadsAndSavesTheForecast() = runTest(dispatcher) {
        enqueue(forecastBody)
        val vm = viewModel()

        vm.selectCity(berlin)
        assertEquals(ForecastState.Loading(berlin, previous = null), vm.forecast.value)
        settle()

        val shown = (vm.forecast.value as ForecastState.Shown).forecast
        assertEquals(2, shown.saved.forecast.hours.size)
        assertFalse(shown.isOutdated)
        assertEquals(shown.saved, cache.saved)
    }

    @Test
    fun forecastWithNoHours_isEmpty() = runTest(dispatcher) {
        enqueue("""{"hourly":{"time":[],"temperature_2m":[],"precipitation_probability":[],"weather_code":[],"wind_speed_10m":[]}}""")
        val vm = viewModel()
        vm.selectCity(berlin)
        settle()
        assertEquals(ForecastState.Empty(berlin), vm.forecast.value)
    }

    @Test
    fun failedRequest_withoutSavedData_isAnError_andRetryRecovers() = runTest(dispatcher) {
        enqueue("oops", code = 500)
        enqueue(forecastBody)
        val vm = viewModel()

        vm.selectCity(berlin)
        settle()
        val error = vm.forecast.value as ForecastState.Error
        assertTrue(error.message.contains("error 500"))

        vm.retryForecast()
        settle()
        assertTrue(vm.forecast.value is ForecastState.Shown)
    }

    @Test
    fun unexpectedResponse_showsItsOwnMessage() = runTest(dispatcher) {
        enqueue("""{"hourly":"nothing here"}""")
        val vm = viewModel()
        vm.selectCity(berlin)
        settle()
        assertTrue((vm.forecast.value as ForecastState.Error).message.contains("can't read"))
    }

    @Test
    fun lossOfConnectivity_showsTheSavedForecast_markedOutdated() = runTest(dispatcher) {
        cache.saved = savedForecast(berlin, fetchedAt = clock - 10 * 60 * 1000)
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)) // startup refresh
        val vm = viewModel()
        settle()

        val shown = (vm.forecast.value as ForecastState.Shown).forecast
        assertEquals(cache.saved, shown.saved)
        assertTrue(shown.isOutdated)
        assertTrue(shown.refreshError!!.startsWith("No internet connection"))
    }

    @Test
    fun savedForecast_isShownAtOnce_whileTheRefreshLoads() = runTest(dispatcher) {
        cache.saved = savedForecast(berlin, fetchedAt = clock - 2 * WalkWeatherViewModel.OUTDATED_AFTER_MILLIS)
        enqueue(forecastBody)
        val vm = viewModel()
        val seen = mutableListOf<ForecastState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.forecast.toList(seen) }
        settle()

        // Saved forecast first, still shown while loading, then the fresh one
        val savedShown = (seen[1] as ForecastState.Shown).forecast
        assertTrue("Older than an hour", savedShown.isOutdated)
        assertEquals(savedShown, (seen[2] as ForecastState.Loading).previous)
        val fresh = (seen.last() as ForecastState.Shown).forecast
        assertFalse(fresh.isOutdated)
        assertNull(fresh.refreshError)
    }

    @Test
    fun anotherCitysSavedForecast_isNeverUsedAsFallback() = runTest(dispatcher) {
        enqueue(forecastBody) // refresh of the saved Lisbon forecast at startup
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        cache.saved = savedForecast(lisbon, fetchedAt = clock)
        val vm = viewModel()
        settle()

        vm.selectCity(berlin)
        settle()
        assertTrue(vm.forecast.value is ForecastState.Error)
    }

    @Test
    fun searchingWhileTheForecastLoads_isNotBlocked() = runTest(dispatcher) {
        server.enqueue(MockResponse().setBody(forecastBody).setBodyDelay(500, java.util.concurrent.TimeUnit.MILLISECONDS))
        val vm = viewModel()

        vm.selectCity(berlin)
        vm.search("Lisbon")
        assertTrue(vm.forecast.value is ForecastState.Loading)
        assertEquals(SearchState.Loading("Lisbon"), vm.search.value)
    }

    @Test
    fun repositoryFailure_keepsTheErrorKind() = runTest(dispatcher) {
        enqueue("nope", code = 404)
        val base = server.url("/")
        val repo = WeatherRepository(OpenMeteoApi(OkHttpClient(), base, base, io = dispatcher), cache) { clock }
        val load = repo.refreshForecast(berlin) as ForecastLoad.Failed
        assertEquals(404, (load.error as com.steppy.walkweather.data.NetworkError.Http).code)
        assertNull(load.saved)
    }
}

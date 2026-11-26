package com.steppy.walkweather.data

import com.steppy.walkweather.domain.City
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Real HTTP against a local MockWebServer: success, empty, failed request, unexpected response, no connection. */
class OpenMeteoApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: OpenMeteoApi

    private val berlin = City(2950159, "Berlin", 52.52437, 13.41053, "State of Berlin", "Germany")

    // Shapes copied from real Open-Meteo responses (fields we don't use trimmed)
    private val searchBody = """
        {"results":[
          {"id":2950159,"name":"Berlin","latitude":52.52437,"longitude":13.41053,"elevation":74.0,
           "country_code":"DE","timezone":"Europe/Berlin","country":"Germany","admin1":"State of Berlin"},
          {"id":5083330,"name":"Berlin","latitude":44.46867,"longitude":-71.18508,
           "country":"United States","admin1":"New Hampshire"}
        ],"generationtime_ms":0.18}
    """.trimIndent()

    private val forecastBody = """
        {"latitude":52.51,"longitude":13.41,"timezone":"Europe/Berlin",
         "hourly_units":{"time":"iso8601","temperature_2m":"°C"},
         "hourly":{"time":["2026-11-26T20:00","2026-11-26T21:00","2026-11-26T22:00"],
                   "temperature_2m":[-0.9,-0.9,-1.2],
                   "precipitation_probability":[0,0,0],
                   "weather_code":[1,0,0],
                   "wind_speed_10m":[9.7,10.8,9.3]}}
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val client = OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build()
        val base = server.url("/")
        api = OpenMeteoApi(client, geocodingBaseUrl = base, forecastBaseUrl = base, io = Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueue(body: String, code: Int = 200) =
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))

    private fun <T> ApiResult<T>.error(): NetworkError {
        assertTrue("Expected a failure, got $this", this is ApiResult.Failure)
        return (this as ApiResult.Failure).error
    }

    // ---- success and empty

    @Test
    fun search_parsesCities_andSendsTheQuery() = runTest {
        enqueue(searchBody)
        val result = api.searchCities("Berlin")

        assertEquals(
            ApiResult.Success(listOf(berlin, City(5083330, "Berlin", 44.46867, -71.18508, "New Hampshire", "United States"))),
            result
        )
        val request = server.takeRequest()
        assertEquals("/v1/search", request.requestUrl!!.encodedPath)
        assertEquals("Berlin", request.requestUrl!!.queryParameter("name"))
    }

    @Test
    fun search_withNoMatches_isAnEmptyList() = runTest {
        // The real API leaves out "results" when nothing matches
        enqueue("""{"generationtime_ms":0.14}""")
        assertEquals(ApiResult.Success(emptyList<City>()), api.searchCities("zzqqxx"))
    }

    @Test
    fun forecast_parsesHours_andAsksForTheCitysPosition() = runTest {
        enqueue(forecastBody)
        val result = api.forecast(berlin) as ApiResult.Success

        assertEquals(3, result.value.hours.size)
        assertEquals(LocalDateTime.of(2026, 11, 26, 20, 0), result.value.hours.first().time)
        assertEquals(-0.9, result.value.hours.first().temperatureC, 0.0)
        val url = server.takeRequest().requestUrl!!
        assertEquals("/v1/forecast", url.encodedPath)
        assertEquals("52.52437", url.queryParameter("latitude"))
        assertEquals("auto", url.queryParameter("timezone"))
    }

    @Test
    fun forecast_leavesOutHoursWithMissingValues() = runTest {
        enqueue(forecastBody.replace("[0,0,0]", "[0,null,0]"))
        val result = api.forecast(berlin) as ApiResult.Success
        assertEquals(2, result.value.hours.size)
    }

    // ---- failed request

    @Test
    fun serverError_isAnHttpError() = runTest {
        enqueue("Service Unavailable", code = 503)
        assertEquals(NetworkError.Http(503, null), api.searchCities("Berlin").error())
    }

    @Test
    fun badRequest_keepsTheApisReason() = runTest {
        enqueue("""{"error":true,"reason":"Latitude must be in range of -90 to 90°. Given: 999.0."}""", code = 400)
        val error = api.forecast(berlin.copy(latitude = 999.0)).error()
        assertEquals(NetworkError.Http(400, "Latitude must be in range of -90 to 90°. Given: 999.0."), error)
    }

    // ---- unexpected response

    @Test
    fun htmlInsteadOfJson_isUnexpected() = runTest {
        enqueue("<html><body>Captive portal login</body></html>")
        assertTrue(api.searchCities("Berlin").error() is NetworkError.UnexpectedResponse)
    }

    @Test
    fun missingField_isUnexpected() = runTest {
        enqueue("""{"latitude":52.5,"longitude":13.4}""")
        assertTrue(api.forecast(berlin).error() is NetworkError.UnexpectedResponse)
    }

    @Test
    fun wrongType_isUnexpected() = runTest {
        enqueue(forecastBody.replace("[-0.9,-0.9,-1.2]", "\"cold\""))
        assertTrue(api.forecast(berlin).error() is NetworkError.UnexpectedResponse)
    }

    @Test
    fun listsOfDifferentLengths_areUnexpected() = runTest {
        enqueue(forecastBody.replace("[9.7,10.8,9.3]", "[9.7]"))
        assertTrue(api.forecast(berlin).error() is NetworkError.UnexpectedResponse)
    }

    @Test
    fun badDate_isUnexpected() = runTest {
        enqueue(forecastBody.replace("2026-11-26T20:00", "tonight"))
        assertTrue(api.forecast(berlin).error() is NetworkError.UnexpectedResponse)
    }

    @Test
    fun emptyBody_isUnexpected() = runTest {
        enqueue("")
        assertTrue(api.searchCities("Berlin").error() is NetworkError.UnexpectedResponse)
    }

    // ---- loss of connectivity

    @Test
    fun connectionDroppedBeforeAnswer_isNoConnection() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertEquals(NetworkError.NoConnection, api.searchCities("Berlin").error())
    }

    @Test
    fun serverUnreachable_isNoConnection() = runTest {
        // Like airplane mode: nothing is listening any more
        server.shutdown()
        assertEquals(NetworkError.NoConnection, api.forecast(berlin).error())
    }

    @Test
    fun noAnswer_isATimeout() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        assertEquals(NetworkError.Timeout, api.searchCities("Berlin").error())
    }
}

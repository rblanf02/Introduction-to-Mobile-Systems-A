package com.steppy.walkweather.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.steppy.walkweather.domain.City
import com.steppy.walkweather.domain.Forecast
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The last forecast that loaded, kept on the phone so it can be shown offline. */
@Serializable
data class CachedForecast(
    val forecast: Forecast,
    /** When it was downloaded (epoch milliseconds). */
    val fetchedAtMillis: Long
)

interface ForecastCache {
    suspend fun load(): CachedForecast?
    suspend fun save(forecast: CachedForecast)
}

/** Keeps one forecast as JSON in a Preferences DataStore. */
class DataStoreForecastCache(private val dataStore: DataStore<Preferences>) : ForecastCache {

    override suspend fun load(): CachedForecast? {
        val text = dataStore.data.first()[KEY] ?: return null
        // A cache from an older app version that no longer reads is simply ignored
        return runCatching { json.decodeFromString(CachedForecast.serializer(), text) }.getOrNull()
    }

    override suspend fun save(forecast: CachedForecast) {
        dataStore.edit { it[KEY] = json.encodeToString(CachedForecast.serializer(), forecast) }
    }

    private companion object {
        val KEY = stringPreferencesKey("last_forecast")
        val json = Json { ignoreUnknownKeys = true }
    }
}

sealed interface ForecastLoad {
    data class Fresh(val forecast: CachedForecast) : ForecastLoad
    /** @param saved the last saved forecast for the same city, to show instead, or null */
    data class Failed(val error: NetworkError, val saved: CachedForecast?) : ForecastLoad
}

class WeatherRepository(
    private val api: OpenMeteoApi,
    private val cache: ForecastCache,
    private val now: () -> Long = System::currentTimeMillis
) {

    suspend fun searchCities(query: String): ApiResult<List<City>> = api.searchCities(query)

    suspend fun savedForecast(): CachedForecast? = cache.load()

    /** Downloads a new forecast and saves it. If that fails, returns the saved one for the same city. */
    suspend fun refreshForecast(city: City): ForecastLoad =
        when (val result = api.forecast(city)) {
            is ApiResult.Success -> {
                val fresh = CachedForecast(result.value, now())
                cache.save(fresh)
                ForecastLoad.Fresh(fresh)
            }
            is ApiResult.Failure -> ForecastLoad.Failed(
                error = result.error,
                // Another city's forecast would be wrong, so it is never used as a fallback
                saved = cache.load()?.takeIf { it.forecast.city.id == city.id }
            )
        }
}

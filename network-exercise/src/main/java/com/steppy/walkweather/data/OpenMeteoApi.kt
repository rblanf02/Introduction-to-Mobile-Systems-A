package com.steppy.walkweather.data

import com.steppy.walkweather.domain.City
import com.steppy.walkweather.domain.Forecast
import com.steppy.walkweather.domain.HourForecast
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

/** Why a request failed. Each one gets its own message and a "Try again" in the UI. */
sealed interface NetworkError {
    /** No connection (airplane mode, no Wi-Fi, server unreachable). */
    data object NoConnection : NetworkError
    data object Timeout : NetworkError
    /** The server answered with an HTTP error, e.g. 400 or 503. */
    data class Http(val code: Int, val reason: String?) : NetworkError
    /** The server answered 200, but not with the data we expect. */
    data class UnexpectedResponse(val detail: String) : NetworkError
}

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val error: NetworkError) : ApiResult<Nothing>
}

/**
 * Client for the free Open-Meteo APIs (https://open-meteo.com, no API key needed).
 * The base URLs can be changed, so tests point them at a local MockWebServer.
 */
class OpenMeteoApi(
    private val client: OkHttpClient,
    private val geocodingBaseUrl: HttpUrl = "https://geocoding-api.open-meteo.com/".toHttpUrl(),
    private val forecastBaseUrl: HttpUrl = "https://api.open-meteo.com/".toHttpUrl(),
    private val io: CoroutineDispatcher = Dispatchers.IO
) {

    /** Places whose name matches [query]. An empty list means nothing was found. */
    suspend fun searchCities(query: String, count: Int = 5): ApiResult<List<City>> {
        val url = geocodingBaseUrl.newBuilder()
            .addPathSegments("v1/search")
            .addQueryParameter("name", query)
            .addQueryParameter("count", count.toString())
            .addQueryParameter("language", "en")
            .addQueryParameter("format", "json")
            .build()
        return get(url) { body ->
            // When nothing matches, the API leaves out "results" entirely
            json.decodeFromString(GeocodingResponse.serializer(), body).results.orEmpty().map {
                City(it.id, it.name, it.latitude, it.longitude, it.admin1, it.country)
            }
        }
    }

    /** The next [hours] hours for [city], in the city's local time. */
    suspend fun forecast(city: City, hours: Int = 12): ApiResult<Forecast> {
        val url = forecastBaseUrl.newBuilder()
            .addPathSegments("v1/forecast")
            .addQueryParameter("latitude", city.latitude.toString())
            .addQueryParameter("longitude", city.longitude.toString())
            .addQueryParameter("hourly", "temperature_2m,precipitation_probability,weather_code,wind_speed_10m")
            .addQueryParameter("forecast_hours", hours.toString())
            .addQueryParameter("timezone", "auto")
            .build()
        return get(url) { body ->
            val hourly = json.decodeFromString(ForecastResponse.serializer(), body).hourly
            Forecast(city, hourly.toHours())
        }
    }

    /** Runs the request off the main thread and turns every kind of failure into a [NetworkError]. */
    private suspend fun <T> get(url: HttpUrl, parse: (String) -> T): ApiResult<T> = withContext(io) {
        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext ApiResult.Failure(NetworkError.Http(response.code, errorReason(body)))
                }
                try {
                    ApiResult.Success(parse(body))
                } catch (e: SerializationException) {
                    unexpected(e)
                } catch (e: IllegalArgumentException) {
                    unexpected(e)
                } catch (e: DateTimeParseException) {
                    unexpected(e)
                }
            }
        } catch (e: SocketTimeoutException) {
            ApiResult.Failure(NetworkError.Timeout)
        } catch (e: IOException) {
            // UnknownHostException, ConnectException, a dropped connection…
            ApiResult.Failure(NetworkError.NoConnection)
        }
    }

    private fun unexpected(e: Exception) =
        ApiResult.Failure(NetworkError.UnexpectedResponse(e.message ?: e.javaClass.simpleName))

    /** Open-Meteo errors look like {"error": true, "reason": "..."}. */
    private fun errorReason(body: String): String? =
        runCatching { json.decodeFromString(ErrorResponse.serializer(), body).reason }.getOrNull()

    private fun HourlyDto.toHours(): List<HourForecast> {
        val sizes = setOf(time.size, temperature.size, precipitationChance.size, weatherCode.size, wind.size)
        require(sizes.size == 1) { "Hourly lists have different lengths: $sizes" }
        return time.indices.mapNotNull { i ->
            // The API sends null for values it doesn't have; such hours are left out
            HourForecast(
                time = LocalDateTime.parse(time[i]),
                temperatureC = temperature[i] ?: return@mapNotNull null,
                precipitationChance = precipitationChance[i] ?: return@mapNotNull null,
                weatherCode = weatherCode[i] ?: return@mapNotNull null,
                windKmh = wind[i] ?: return@mapNotNull null
            )
        }
    }

    private companion object {
        /** Ignores the fields we don't use, but a missing or wrong-typed field we need is an error. */
        val json = Json { ignoreUnknownKeys = true }
    }
}

@Serializable
private data class GeocodingResponse(val results: List<GeocodingResult>? = null)

@Serializable
private data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null
)

@Serializable
private data class ForecastResponse(val hourly: HourlyDto)

@Serializable
private data class HourlyDto(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature: List<Double?>,
    @SerialName("precipitation_probability") val precipitationChance: List<Int?>,
    @SerialName("weather_code") val weatherCode: List<Int?>,
    @SerialName("wind_speed_10m") val wind: List<Double?>
)

@Serializable
private data class ErrorResponse(val reason: String? = null)

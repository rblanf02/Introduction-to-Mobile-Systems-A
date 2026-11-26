package com.steppy.walkweather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.steppy.walkweather.data.ApiResult
import com.steppy.walkweather.data.CachedForecast
import com.steppy.walkweather.data.ForecastLoad
import com.steppy.walkweather.data.NetworkError
import com.steppy.walkweather.data.WeatherRepository
import com.steppy.walkweather.domain.City
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SearchState {
    data object Idle : SearchState
    data class Loading(val query: String) : SearchState
    data class Results(val query: String, val cities: List<City>) : SearchState
    /** The search worked but found no place with that name. */
    data class Empty(val query: String) : SearchState
    data class Error(val query: String, val message: String) : SearchState
}

/**
 * A forecast on screen.
 *
 * @param isOutdated older than [WalkWeatherViewModel.OUTDATED_AFTER_MILLIS], or the latest refresh failed
 * @param refreshError why the latest refresh failed, when this is the saved forecast shown instead
 */
data class ForecastUi(
    val saved: CachedForecast,
    val isOutdated: Boolean,
    val refreshError: String? = null
)

sealed interface ForecastState {
    /** No city picked yet and nothing saved. */
    data object NoCity : ForecastState
    /** @param previous the forecast still shown while the new one loads, if any */
    data class Loading(val city: City, val previous: ForecastUi?) : ForecastState
    data class Shown(val forecast: ForecastUi) : ForecastState
    /** The request worked but the forecast has no hours. */
    data class Empty(val city: City) : ForecastState
    /** Failed and there is no saved forecast for this city. */
    data class Error(val city: City, val message: String) : ForecastState
}

/**
 * Search and forecast load independently, each in its own coroutine, so one loading never
 * blocks the other or the rest of the screen.
 */
class WalkWeatherViewModel(
    private val repository: WeatherRepository,
    private val now: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _search = MutableStateFlow<SearchState>(SearchState.Idle)
    val search: StateFlow<SearchState> = _search.asStateFlow()

    private val _forecast = MutableStateFlow<ForecastState>(ForecastState.NoCity)
    val forecast: StateFlow<ForecastState> = _forecast.asStateFlow()

    private var searchJob: Job? = null
    private var forecastJob: Job? = null

    init {
        // Show the saved forecast at once (works offline), then try to refresh it
        viewModelScope.launch {
            val saved = repository.savedForecast() ?: return@launch
            if (_forecast.value != ForecastState.NoCity) return@launch
            _forecast.value = ForecastState.Shown(ForecastUi(saved, isOutdated = isOld(saved)))
            loadForecast(saved.forecast.city)
        }
    }

    fun search(query: String) {
        val clean = query.trim()
        if (clean.length < 2) {
            _search.value = SearchState.Idle
            return
        }
        // A new search replaces one still running
        searchJob?.cancel()
        _search.value = SearchState.Loading(clean)
        searchJob = viewModelScope.launch {
            _search.value = when (val result = repository.searchCities(clean)) {
                is ApiResult.Success ->
                    if (result.value.isEmpty()) SearchState.Empty(clean) else SearchState.Results(clean, result.value)
                is ApiResult.Failure -> SearchState.Error(clean, messageFor(result.error))
            }
        }
    }

    fun retrySearch() {
        val query = when (val state = _search.value) {
            is SearchState.Error -> state.query
            is SearchState.Empty -> state.query
            else -> return
        }
        search(query)
    }

    fun selectCity(city: City) {
        _search.value = SearchState.Idle
        loadForecast(city)
    }

    fun retryForecast() {
        val city = when (val state = _forecast.value) {
            is ForecastState.Error -> state.city
            is ForecastState.Empty -> state.city
            is ForecastState.Shown -> state.forecast.saved.forecast.city
            else -> return
        }
        loadForecast(city)
    }

    private fun loadForecast(city: City) {
        forecastJob?.cancel()
        val previous = (_forecast.value as? ForecastState.Shown)?.forecast
            ?.takeIf { it.saved.forecast.city.id == city.id }
        _forecast.value = ForecastState.Loading(city, previous)
        forecastJob = viewModelScope.launch {
            _forecast.value = when (val load = repository.refreshForecast(city)) {
                is ForecastLoad.Fresh ->
                    if (load.forecast.forecast.hours.isEmpty()) ForecastState.Empty(city)
                    else ForecastState.Shown(ForecastUi(load.forecast, isOutdated = false))
                is ForecastLoad.Failed ->
                    if (load.saved != null && load.saved.forecast.hours.isNotEmpty()) {
                        ForecastState.Shown(ForecastUi(load.saved, isOutdated = true, refreshError = messageFor(load.error)))
                    } else {
                        ForecastState.Error(city, messageFor(load.error))
                    }
            }
        }
    }

    private fun isOld(saved: CachedForecast) = now() - saved.fetchedAtMillis > OUTDATED_AFTER_MILLIS

    class Factory(private val repository: WeatherRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = WalkWeatherViewModel(repository) as T
    }

    companion object {
        /** Forecasts change through the day, so a saved one counts as outdated after an hour. */
        const val OUTDATED_AFTER_MILLIS = 60 * 60 * 1000L

        fun messageFor(error: NetworkError): String = when (error) {
            NetworkError.NoConnection -> "No internet connection. Check Wi-Fi or mobile data and try again."
            NetworkError.Timeout -> "The weather service took too long to answer. Try again."
            is NetworkError.Http ->
                if (error.code >= 500) "The weather service is having problems (error ${error.code}). Try again later."
                else "The weather service refused the request (error ${error.code})."
            is NetworkError.UnexpectedResponse -> "The weather service sent data this app can't read. Try again later."
        }
    }
}

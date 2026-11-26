package com.steppy.walkweather.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steppy.walkweather.data.CachedForecast
import com.steppy.walkweather.domain.City
import com.steppy.walkweather.domain.HourForecast
import com.steppy.walkweather.domain.WalkAdvice
import com.steppy.walkweather.domain.WalkRating
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun WalkWeatherScreen(viewModel: WalkWeatherViewModel) {
    val search by viewModel.search.collectAsStateWithLifecycle()
    val forecast by viewModel.forecast.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Walk Weather", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Is it a good time for a walk? Weather from Open-Meteo.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            item {
                // Stays usable while a search or forecast is loading
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("City") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.search(query) }),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.search(query) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Search") }
                }
            }
            searchSection(search, onRetry = viewModel::retrySearch, onSelect = viewModel::selectCity)
            item { HorizontalDivider() }
            item { ForecastSection(forecast, onRetry = viewModel::retryForecast) }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.searchSection(
    state: SearchState,
    onRetry: () -> Unit,
    onSelect: (City) -> Unit
) {
    when (state) {
        SearchState.Idle -> Unit
        is SearchState.Loading -> item { LoadingRow("Searching for \"${state.query}\"…") }
        is SearchState.Empty -> item {
            MessageCard(
                title = "No places found",
                body = "Nothing matches \"${state.query}\". Check the spelling or try a bigger city nearby.",
                action = "Search again",
                onAction = onRetry
            )
        }
        is SearchState.Error -> item {
            MessageCard(title = "Search failed", body = state.message, action = "Try again", onAction = onRetry, isError = true)
        }
        is SearchState.Results -> items(state.cities, key = { it.id }) { city ->
            Text(
                text = city.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { onSelect(city) }
                    .padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun ForecastSection(state: ForecastState, onRetry: () -> Unit) {
    when (state) {
        ForecastState.NoCity -> MessageCard(
            title = "No forecast yet",
            body = "Search for a city to see the next 12 hours and the best time for a walk."
        )
        is ForecastState.Loading -> Column {
            LoadingRow("Loading the forecast for ${state.city.name}…")
            // The previous forecast stays visible while the new one loads
            state.previous?.let { ForecastCard(it.saved, isOutdated = it.isOutdated, refreshError = null) }
        }
        is ForecastState.Empty -> MessageCard(
            title = "No forecast for ${state.city.name}",
            body = "The weather service has no hourly data for this place right now.",
            action = "Try again",
            onAction = onRetry
        )
        is ForecastState.Error -> MessageCard(
            title = "Couldn't load the forecast for ${state.city.name}",
            body = state.message,
            action = "Try again",
            onAction = onRetry,
            isError = true
        )
        is ForecastState.Shown -> ForecastCard(
            state.forecast.saved,
            isOutdated = state.forecast.isOutdated,
            refreshError = state.forecast.refreshError,
            onRetry = onRetry
        )
    }
}

@Composable
private fun ForecastCard(
    saved: CachedForecast,
    isOutdated: Boolean,
    refreshError: String?,
    onRetry: (() -> Unit)? = null
) {
    val forecast = saved.forecast
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(forecast.city.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            val updated = "Last updated ${formatFetchedAt(saved.fetchedAtMillis)}"
            if (isOutdated) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Saved forecast — may be outdated", fontWeight = FontWeight.Bold)
                        Text(updated)
                        refreshError?.let { Text(it) }
                        if (onRetry != null && refreshError != null) {
                            OutlinedButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text("Try again")
                            }
                        }
                    }
                }
            } else {
                Text(updated, style = MaterialTheme.typography.bodySmall)
            }

            val best = WalkAdvice.bestHour(forecast.hours)
            Text(
                text = best?.let { "Best time for a walk: ${it.time.format(HOUR)}" }
                    ?: "No good time for a walk in the next hours",
                style = MaterialTheme.typography.titleMedium
            )
            forecast.hours.forEach { HourRow(it) }
        }
    }
}

@Composable
private fun HourRow(hour: HourForecast) {
    val rating = WalkAdvice.rate(hour)
    val temp = "${hour.temperatureC.roundToInt()}°C"
    val description = WalkAdvice.describe(hour.weatherCode)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "${hour.time.format(HOUR)}, $description, $temp, " +
                    "${hour.precipitationChance} percent chance of rain, ${rating.label}"
            },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(hour.time.format(HOUR), fontWeight = FontWeight.SemiBold, modifier = Modifier.width(56.dp))
        Text("$description · $temp · ${hour.precipitationChance}% rain", modifier = Modifier.weight(1f))
        Text(
            when (rating) {
                WalkRating.GOOD -> "👍 Walk"
                WalkRating.OK -> "🧥 OK"
                WalkRating.POOR -> "🏠 Stay in"
            }
        )
    }
}

@Composable
private fun LoadingRow(text: String) {
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text(text)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
private fun MessageCard(
    title: String,
    body: String,
    action: String? = null,
    onAction: () -> Unit = {},
    isError: Boolean = false
) {
    val colors = if (isError) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    } else CardDefaults.cardColors()
    Card(
        colors = colors,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body)
            if (action != null) {
                Button(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(action) }
            }
        }
    }
}

private val HOUR: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val FETCHED_AT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm")

private fun formatFetchedAt(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(FETCHED_AT)

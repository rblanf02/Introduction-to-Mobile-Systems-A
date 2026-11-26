# Walk Weather — Week 7 networking exercise

Steppy runs completely on the phone and has no network access (no `INTERNET` permission). The course brief says that in that case we *"complete a separate small networking exercise rather than adding an unnecessary feature"*. This module is that exercise: a small separate app, built from the same Gradle project, that is **not** part of Steppy.

**What it does:** search for a city, then see the next 12 hours of weather and the best time for a walk. Data comes from the free [Open-Meteo](https://open-meteo.com/) APIs (no account or API key):

- Geocoding: `GET https://geocoding-api.open-meteo.com/v1/search?name=<city>&count=5`
- Forecast: `GET https://api.open-meteo.com/v1/forecast?latitude=..&longitude=..&hourly=temperature_2m,precipitation_probability,weather_code,wind_speed_10m&forecast_hours=12&timezone=auto`

## Build, run and test

```bash
ANDROID_HOME=$HOME/Android/Sdk bash gradlew :network-exercise:installDebug
ANDROID_HOME=$HOME/Android/Sdk bash gradlew :network-exercise:testDebugUnitTest
```

In Android Studio, pick the **network-exercise** run configuration. The app is installed as "Walk Weather" next to Steppy.

## How it meets the brief

| Requirement | Where |
|---|---|
| **R1** Connect to a suitable API and inspect the request and response | `data/OpenMeteoApi.kt` (OkHttp). Debug builds log every request and response, with the JSON body, to Logcat (tag `OkHttp`). The same calls appear in Android Studio's **App Inspection → Network Inspector** |
| **R2** Loading, success, empty and error states, without blocking the interface | `ui/WalkWeatherViewModel.kt` has separate `SearchState` and `ForecastState`. Requests run on `Dispatchers.IO`; search and forecast load independently, so you can search while a forecast loads. Empty = no city matches, or no hourly data |
| **R3** Keep useful information locally, and say when it may be outdated | `data/WeatherRepository.kt` saves the last forecast in DataStore. It is shown at startup (also offline) and refreshed. It is marked **"Saved forecast — may be outdated"** with the time it was downloaded when it is over an hour old or the refresh failed |
| **R4** Test a failed request, an unexpected response and loss of connectivity | Unit tests with a local `MockWebServer` (real HTTP), below |
| **R5** Retry or alternative path | "Try again" on every error, empty and outdated state; the saved forecast is the alternative when offline. Another city's saved forecast is never shown in its place |

## Tests (35, `src/test`)

- `OpenMeteoApiTest` (15): success and empty search, forecast parsing, **failed request** (HTTP 503, HTTP 400 with the API's reason), **unexpected response** (HTML instead of JSON, missing field, wrong type, lists of different lengths, bad date, empty body), **loss of connectivity** (connection dropped, server unreachable) and timeout.
- `WalkWeatherViewModelTest` (13): every UI state, retry after each failure, the saved forecast shown when offline, and search not blocked while the forecast loads.
- `WalkAdviceTest` (7): the walking advice rules.

## Manual checks on a phone (for the report)

1. Search "Berlin": the loading row, then the results. Search "zzqqxx": the empty state.
2. Pick a city: the forecast and "Best time for a walk". Screenshot Logcat (`OkHttp`) or the Network Inspector showing the request URL and JSON response.
3. Turn on **airplane mode**, then search: "No internet connection" with "Try again".
4. With airplane mode still on, close and reopen the app: the saved forecast appears, marked "may be outdated", with the download time.
5. Turn airplane mode off and tap "Try again": the forecast refreshes and the warning disappears.

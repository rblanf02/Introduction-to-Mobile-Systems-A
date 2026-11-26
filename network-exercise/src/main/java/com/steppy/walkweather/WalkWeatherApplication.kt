package com.steppy.walkweather

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.steppy.walkweather.data.DataStoreForecastCache
import com.steppy.walkweather.data.OpenMeteoApi
import com.steppy.walkweather.data.WeatherRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/** Builds the app's dependencies once (manual dependency injection, as in Steppy). */
class WalkWeatherApplication : Application() {

    lateinit var repository: WeatherRepository
        private set

    override fun onCreate() {
        super.onCreate()

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    // Logs every request and response (URL, headers, JSON body) to Logcat, tag "OkHttp".
                    // Android Studio's Network Inspector shows the same calls.
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                }
            }
            .build()

        val cache = DataStoreForecastCache(
            PreferenceDataStoreFactory.create { preferencesDataStoreFile("walk_weather_cache") }
        )
        repository = WeatherRepository(OpenMeteoApi(client), cache)
    }
}

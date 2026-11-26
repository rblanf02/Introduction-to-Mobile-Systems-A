package com.steppy.walkweather.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime

/** A place found by the geocoding search. */
@Serializable
data class City(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    /** State or region, when the API gives one. */
    val region: String? = null,
    val country: String? = null
) {
    /** "Berlin, State of Berlin, Germany" — tells apart places with the same name. */
    val label: String get() = listOfNotNull(name, region, country).distinct().joinToString(", ")
}

/** The weather for one hour, in the city's local time. */
@Serializable
data class HourForecast(
    @Serializable(with = LocalDateTimeIsoSerializer::class)
    val time: LocalDateTime,
    val temperatureC: Double,
    /** 0–100 %. */
    val precipitationChance: Int,
    /** WMO weather code. */
    val weatherCode: Int,
    val windKmh: Double
)

@Serializable
data class Forecast(
    val city: City,
    val hours: List<HourForecast>
)

/** Writes a [LocalDateTime] as ISO text, e.g. "2026-11-26T20:00". */
object LocalDateTimeIsoSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDateTimeIso", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDateTime = LocalDateTime.parse(decoder.decodeString())
}

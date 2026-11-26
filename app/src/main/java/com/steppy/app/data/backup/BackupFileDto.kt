package com.steppy.app.data.backup

import kotlinx.serialization.Serializable

// The backup file as it is written. Dates are ISO strings (yyyy-MM-dd) and enums are their names,
// so the file stays readable and doesn't change when Room or the UI classes change.
// Change a field only together with BackupCodec.VERSION.

@Serializable
internal data class BackupFileDto(
    val format: String,
    val version: Int,
    /** CRC32 of [data], as 8 hex digits. */
    val checksum: String,
    val data: BackupDataDto
)

@Serializable
internal data class BackupDataDto(
    val createdOn: String,
    val settings: SettingsDto,
    val progress: ProgressDto,
    val baseline: BaselineDto? = null,
    val days: List<DayDto> = emptyList(),
    val challenges: List<UserChallengeDto> = emptyList()
)

@Serializable
internal data class SettingsDto(
    val onboarded: Boolean,
    val username: String,
    val mascot: String,
    val mainGoal: String,
    val activityLevel: String,
    val dailyGoal: Int,
    val manualMode: Boolean,
    val reminders: Boolean
)

@Serializable
internal data class ProgressDto(
    val todayDate: String? = null,
    val todaySteps: Int,
    val streak: Int,
    val xp: Int,
    val activeChallenges: List<String> = emptyList(),
    val completedChallenges: List<String> = emptyList()
)

@Serializable
internal data class BaselineDto(
    val asOf: String,
    val xpBefore: Int,
    val streakBefore: Int,
    val heartsBefore: Int,
    val activeDaysBefore: Int
)

@Serializable
internal data class DayDto(
    val date: String,
    val steps: Int,
    val stepsBeforeNoon: Int? = null,
    val goal: Int,
    val source: String
)

@Serializable
internal data class UserChallengeDto(
    val id: String,
    val activatedOn: String,
    val completedOn: String? = null
)

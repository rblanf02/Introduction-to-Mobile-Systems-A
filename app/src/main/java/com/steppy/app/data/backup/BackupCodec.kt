package com.steppy.app.data.backup

import com.steppy.app.data.local.DailyRecord
import com.steppy.app.data.local.ProgressBaseline
import com.steppy.app.data.local.StepSource
import com.steppy.app.data.local.UserChallenge
import com.steppy.app.data.repository.ProgressSnapshot
import com.steppy.app.data.settings.UserSettings
import com.steppy.app.domain.ActivityLevel
import com.steppy.app.domain.ChallengeCatalogue
import com.steppy.app.domain.ChallengeEvaluator
import com.steppy.app.domain.MainMotivation
import com.steppy.app.ui.Mascot
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.zip.CRC32

/** Everything stored in Room: the daily records, activated challenges and the baseline. */
data class HistoryData(
    val baseline: ProgressBaseline?,
    val days: List<DailyRecord>,
    val challenges: List<UserChallenge>
)

/** A full backup: settings, today's progress and the whole history. */
data class Backup(
    val createdOn: LocalDate,
    val settings: UserSettings,
    val progress: ProgressSnapshot,
    val history: HistoryData
)

sealed interface BackupReadResult {
    data class Valid(val backup: Backup) : BackupReadResult
    /** @param reason shown to the user */
    data class Invalid(val reason: String) : BackupReadResult
}

/**
 * Turns a [Backup] into the JSON backup file and back (PROJECT_PLAN §5 "Data backup and transfer").
 *
 * The file has a format name, a version and a CRC32 checksum of the data. Reading checks all three
 * and every value range, so a damaged, edited or foreign file is refused before anything is replaced.
 */
object BackupCodec {

    const val FORMAT = "steppy-backup"
    const val VERSION = 1

    /** Larger files are refused without reading them (10 years of history is under 1 MB). */
    const val MAX_FILE_BYTES = 5 * 1024 * 1024

    const val MAX_STEPS = 200_000
    val GOAL_RANGE = 500..50_000
    const val MAX_HEARTS = 2
    const val MAX_USERNAME_LENGTH = 50

    private val fileJson = Json { prettyPrint = true; encodeDefaults = true }

    /** Compact and fixed, so the checksum doesn't depend on how the file is formatted. */
    private val checksumJson = Json { encodeDefaults = true }

    fun encode(backup: Backup): String {
        val data = backup.toDto()
        return fileJson.encodeToString(
            BackupFileDto.serializer(),
            BackupFileDto(FORMAT, VERSION, checksum(data), data)
        )
    }

    fun decode(text: String): BackupReadResult {
        if (text.isBlank()) return invalid("The file is empty.")
        if (text.toByteArray(Charsets.UTF_8).size > MAX_FILE_BYTES) {
            return invalid("The file is too large to be a Steppy backup.")
        }
        val file = try {
            fileJson.decodeFromString(BackupFileDto.serializer(), text)
        } catch (e: SerializationException) {
            return invalid(NOT_A_BACKUP)
        } catch (e: IllegalArgumentException) {
            return invalid(NOT_A_BACKUP)
        }
        if (file.format != FORMAT) return invalid(NOT_A_BACKUP)
        if (file.version > VERSION) return invalid("This backup was made by a newer version of Steppy. Update the app first.")
        if (file.version != VERSION) return invalid(NOT_A_BACKUP)
        if (file.checksum != checksum(file.data)) return invalid("The file is damaged or was changed after it was saved.")

        return try {
            BackupReadResult.Valid(file.data.toBackup())
        } catch (e: InvalidBackupException) {
            invalid(e.message ?: NOT_A_BACKUP)
        } catch (e: DateTimeParseException) {
            invalid("The file contains a date that isn't valid.")
        }
    }

    private const val NOT_A_BACKUP = "This isn't a Steppy backup file."

    private fun invalid(reason: String) = BackupReadResult.Invalid(reason)

    private fun checksum(data: BackupDataDto): String {
        val crc = CRC32()
        crc.update(checksumJson.encodeToString(BackupDataDto.serializer(), data).toByteArray(Charsets.UTF_8))
        return "%08x".format(crc.value)
    }

    private class InvalidBackupException(message: String) : Exception(message)

    private fun check(condition: Boolean, message: () -> String) {
        if (!condition) throw InvalidBackupException(message())
    }

    private inline fun <reified E : Enum<E>> enumNamed(name: String, what: String): E =
        enumValues<E>().firstOrNull { it.name == name }
            ?: throw InvalidBackupException("The file contains an unknown $what.")

    // ---- Backup -> file

    private fun Backup.toDto() = BackupDataDto(
        createdOn = createdOn.toString(),
        settings = SettingsDto(
            onboarded = settings.isOnboardingCompleted,
            username = settings.username,
            mascot = settings.mascot.name,
            mainGoal = settings.mainGoal.name,
            activityLevel = settings.activityLevel.name,
            dailyGoal = settings.dailyGoal,
            manualMode = settings.isManualMode,
            reminders = settings.remindersEnabled
        ),
        progress = ProgressDto(
            todayDate = progress.todayDate?.toString(),
            todaySteps = progress.todaySteps,
            streak = progress.streak,
            xp = progress.baseXp,
            activeChallenges = progress.activeChallengeIds.sorted(),
            completedChallenges = progress.completedChallengeIds.sorted()
        ),
        baseline = history.baseline?.let {
            BaselineDto(
                asOf = it.asOf.toString(),
                xpBefore = it.xpBefore,
                streakBefore = it.streakBefore,
                heartsBefore = it.heartsBefore,
                activeDaysBefore = it.activeDaysBefore
            )
        },
        days = history.days.sortedBy { it.date }.map {
            DayDto(
                date = it.date.toString(),
                steps = it.steps,
                stepsBeforeNoon = it.stepsBeforeNoon,
                goal = it.goal,
                source = it.source.name
            )
        },
        challenges = history.challenges.sortedBy { it.challengeId }.map {
            UserChallengeDto(
                id = it.challengeId,
                activatedOn = it.activatedOn.toString(),
                completedOn = it.completedOn?.toString()
            )
        }
    )

    // ---- File -> Backup, checking every value

    private fun BackupDataDto.toBackup(): Backup = Backup(
        createdOn = LocalDate.parse(createdOn),
        settings = settings.toSettings(),
        progress = progress.toSnapshot(),
        history = HistoryData(
            baseline = baseline?.toBaseline(),
            days = days.map { it.toRecord() }.also { records ->
                check(records.map { it.date }.toSet().size == records.size) { "The file has the same day twice." }
            },
            challenges = challenges.map { it.toUserChallenge() }.also { list ->
                check(list.map { it.challengeId }.toSet().size == list.size) { "The file has the same challenge twice." }
            }
        )
    )

    private fun SettingsDto.toSettings(): UserSettings {
        check(username.isNotBlank() && username.length <= MAX_USERNAME_LENGTH) { "The nickname in the file isn't valid." }
        check(dailyGoal in GOAL_RANGE) { "The daily goal in the file isn't valid." }
        return UserSettings(
            isOnboardingCompleted = onboarded,
            username = username,
            mascot = enumNamed<Mascot>(mascot, "mascot"),
            mainGoal = enumNamed<MainMotivation>(mainGoal, "main goal"),
            activityLevel = enumNamed<ActivityLevel>(activityLevel, "activity level"),
            dailyGoal = dailyGoal,
            isManualMode = manualMode,
            remindersEnabled = reminders
        )
    }

    private fun ProgressDto.toSnapshot(): ProgressSnapshot {
        check(todaySteps in 0..MAX_STEPS) { "Today's steps in the file aren't valid." }
        check(streak >= 0 && xp >= 0) { "The streak or XP in the file isn't valid." }
        val known = (activeChallenges + completedChallenges).all { ChallengeCatalogue.byId(it) != null }
        check(known) { "The file contains an unknown challenge." }
        val inProgress = activeChallenges.count { it !in completedChallenges }
        check(inProgress <= ChallengeEvaluator.MAX_ACTIVE) { "The file has too many active challenges." }
        return ProgressSnapshot(
            todayDate = todayDate?.let { LocalDate.parse(it) },
            todaySteps = todaySteps,
            streak = streak,
            baseXp = xp,
            activeChallengeIds = activeChallenges.toSet(),
            completedChallengeIds = completedChallenges.toSet()
        )
    }

    private fun BaselineDto.toBaseline(): ProgressBaseline {
        check(xpBefore >= 0 && streakBefore >= 0 && activeDaysBefore >= 0 && heartsBefore in 0..MAX_HEARTS) {
            "The saved progress in the file isn't valid."
        }
        return ProgressBaseline(
            asOf = LocalDate.parse(asOf),
            xpBefore = xpBefore,
            streakBefore = streakBefore,
            heartsBefore = heartsBefore,
            activeDaysBefore = activeDaysBefore
        )
    }

    private fun DayDto.toRecord(): DailyRecord {
        check(steps in 0..MAX_STEPS) { "The steps for $date aren't valid." }
        check(stepsBeforeNoon == null || stepsBeforeNoon in 0..steps) { "The morning steps for $date aren't valid." }
        check(goal in GOAL_RANGE) { "The goal for $date isn't valid." }
        return DailyRecord(
            date = LocalDate.parse(date),
            steps = steps,
            stepsBeforeNoon = stepsBeforeNoon,
            goal = goal,
            source = enumNamed<StepSource>(source, "step source")
        )
    }

    private fun UserChallengeDto.toUserChallenge(): UserChallenge {
        check(ChallengeCatalogue.byId(id) != null) { "The file contains an unknown challenge." }
        val activated = LocalDate.parse(activatedOn)
        val completed = completedOn?.let { LocalDate.parse(it) }
        check(completed == null || !completed.isBefore(activated)) { "A challenge in the file ends before it starts." }
        return UserChallenge(challengeId = id, activatedOn = activated, completedOn = completed)
    }
}

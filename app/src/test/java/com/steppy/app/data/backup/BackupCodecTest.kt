package com.steppy.app.data.backup

import com.steppy.app.data.local.DailyRecord
import com.steppy.app.data.local.ProgressBaseline
import com.steppy.app.data.local.StepSource
import com.steppy.app.data.local.UserChallenge
import com.steppy.app.data.repository.ProgressSnapshot
import com.steppy.app.data.settings.UserSettings
import com.steppy.app.domain.ActivityLevel
import com.steppy.app.domain.MainMotivation
import com.steppy.app.ui.Mascot
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.zip.CRC32

class BackupCodecTest {

    private val nov24 = LocalDate.of(2026, 11, 24)
    private val nov25 = nov24.plusDays(1)

    private val backup = Backup(
        createdOn = nov25,
        settings = UserSettings(
            isOnboardingCompleted = true,
            username = "Test walker",
            mascot = Mascot.IGUANA,
            mainGoal = MainMotivation.BUILD_STREAK,
            activityLevel = ActivityLevel.VERY_ACTIVE,
            dailyGoal = 8000,
            isManualMode = false,
            remindersEnabled = true
        ),
        progress = ProgressSnapshot(
            todayDate = nov25,
            todaySteps = 4200,
            streak = 9,
            baseXp = 1234,
            activeChallengeIds = setOf("c1", "c2"),
            completedChallengeIds = setOf("c2")
        ),
        history = HistoryData(
            baseline = ProgressBaseline.newUser(nov24),
            days = listOf(
                DailyRecord(nov24, 9100, stepsBeforeNoon = 2500, goal = 6000, source = StepSource.SENSOR),
                DailyRecord(nov25, 4200, stepsBeforeNoon = null, goal = 8000, source = StepSource.MANUAL)
            ),
            challenges = listOf(
                UserChallenge("c1", activatedOn = nov24, completedOn = null),
                UserChallenge("c2", activatedOn = nov24, completedOn = nov25)
            )
        )
    )

    private fun decodeValid(text: String): Backup {
        val result = BackupCodec.decode(text)
        assertTrue("Expected a valid backup, got $result", result is BackupReadResult.Valid)
        return (result as BackupReadResult.Valid).backup
    }

    private fun assertInvalid(text: String, reasonContains: String) {
        val result = BackupCodec.decode(text)
        assertTrue("Expected invalid, got $result", result is BackupReadResult.Invalid)
        val reason = (result as BackupReadResult.Invalid).reason
        assertTrue("\"$reason\" should mention \"$reasonContains\"", reason.contains(reasonContains))
    }

    /** Writes [data] as a file with a correct checksum, to test the value checks behind it. */
    private fun fileWithData(data: BackupDataDto): String {
        val compact = Json { encodeDefaults = true }.encodeToString(BackupDataDto.serializer(), data)
        val crc = CRC32().apply { update(compact.toByteArray(Charsets.UTF_8)) }
        return Json.encodeToString(
            BackupFileDto.serializer(),
            BackupFileDto(BackupCodec.FORMAT, BackupCodec.VERSION, "%08x".format(crc.value), data)
        )
    }

    private fun validData(): BackupDataDto =
        Json.decodeFromString(BackupFileDto.serializer(), BackupCodec.encode(backup)).data

    @Test
    fun roundTrip_keepsEverything() {
        assertEquals(backup, decodeValid(BackupCodec.encode(backup)))
    }

    @Test
    fun roundTrip_ofANewUserWithNoHistory() {
        val empty = Backup(
            createdOn = nov25,
            settings = UserSettings(),
            progress = ProgressSnapshot(null, 0, 0, 0, emptySet(), emptySet()),
            history = HistoryData(null, emptyList(), emptyList())
        )
        assertEquals(empty, decodeValid(BackupCodec.encode(empty)))
    }

    @Test
    fun file_isReadableJsonWithFormatAndVersion() {
        val text = BackupCodec.encode(backup)
        assertTrue(text.contains("\"format\": \"steppy-backup\""))
        assertTrue(text.contains("\"version\": 1"))
        assertTrue(text.contains("\"date\": \"2026-11-24\""))
    }

    @Test
    fun reformattingTheFile_isStillValid() {
        val compact = Json.parseToJsonElement(BackupCodec.encode(backup)).toString()
        assertEquals(backup, decodeValid(compact))
    }

    @Test
    fun emptyFile_isRefused() {
        assertInvalid("", "empty")
        assertInvalid("  \n", "empty")
    }

    @Test
    fun otherFiles_areRefused() {
        assertInvalid("hello", "isn't a Steppy backup")
        assertInvalid("{\"name\": \"something else\"}", "isn't a Steppy backup")
        assertInvalid("[1, 2, 3]", "isn't a Steppy backup")
        assertInvalid(BackupCodec.encode(backup).replace("steppy-backup", "other-app"), "isn't a Steppy backup")
    }

    @Test
    fun cutOffFile_isRefused() {
        val text = BackupCodec.encode(backup)
        assertInvalid(text.substring(0, text.length / 2), "isn't a Steppy backup")
    }

    @Test
    fun newerVersion_asksToUpdateTheApp() {
        assertInvalid(BackupCodec.encode(backup).replace("\"version\": 1", "\"version\": 2"), "newer version")
    }

    @Test
    fun editedValue_failsTheChecksum() {
        assertInvalid(BackupCodec.encode(backup).replace("9100", "91000"), "damaged")
    }

    @Test
    fun tooLargeFile_isRefused() {
        assertInvalid("x".repeat(BackupCodec.MAX_FILE_BYTES + 1), "too large")
    }

    @Test
    fun valuesOutOfRange_areRefused() {
        val data = validData()
        assertInvalid(
            fileWithData(data.copy(days = data.days.map { it.copy(steps = 300_000) })),
            "steps for 2026-11-24"
        )
        assertInvalid(fileWithData(data.copy(settings = data.settings.copy(dailyGoal = 100))), "daily goal")
        assertInvalid(fileWithData(data.copy(settings = data.settings.copy(username = " "))), "nickname")
        assertInvalid(fileWithData(data.copy(progress = data.progress.copy(streak = -1))), "streak")
        assertInvalid(fileWithData(data.copy(baseline = data.baseline!!.copy(heartsBefore = 3))), "saved progress")
        assertInvalid(
            fileWithData(data.copy(days = data.days.map { it.copy(stepsBeforeNoon = it.steps + 1) })),
            "morning steps"
        )
    }

    @Test
    fun unknownNames_areRefused() {
        val data = validData()
        assertInvalid(fileWithData(data.copy(settings = data.settings.copy(mascot = "DRAGON"))), "unknown mascot")
        assertInvalid(fileWithData(data.copy(days = data.days.map { it.copy(source = "GPS") })), "unknown step source")
        assertInvalid(
            fileWithData(data.copy(progress = data.progress.copy(activeChallenges = listOf("c99")))),
            "unknown challenge"
        )
    }

    @Test
    fun badDates_areRefused() {
        val data = validData()
        assertInvalid(fileWithData(data.copy(createdOn = "yesterday")), "date")
        assertInvalid(fileWithData(data.copy(days = data.days.map { it.copy(date = "2026-13-01") })), "date")
    }

    @Test
    fun inconsistentData_isRefused() {
        val data = validData()
        assertInvalid(fileWithData(data.copy(days = data.days + data.days.first())), "same day twice")
        assertInvalid(
            fileWithData(data.copy(progress = data.progress.copy(activeChallenges = listOf("c1", "c3", "c4", "c5")))),
            "too many active"
        )
        assertInvalid(
            fileWithData(
                data.copy(challenges = listOf(UserChallengeDto("c1", activatedOn = "2026-11-25", completedOn = "2026-11-24")))
            ),
            "ends before it starts"
        )
    }
}

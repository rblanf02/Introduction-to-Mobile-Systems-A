package com.steppy.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ChallengeCatalogueTest {

    private val inputs = ChallengeProgressInputs(
        todaySteps = 4000,
        streak = 3,
        weekSteps = 21000,
        weekendSteps = 9000,
        stepsBeforeNoon = 1500
    )

    @Test
    fun `ids are unique and stable`() {
        assertEquals(listOf("c1", "c2", "c3", "c4", "c5"), ChallengeCatalogue.ALL.map { it.id })
    }

    @Test
    fun `every challenge has a positive target and reward`() {
        ChallengeCatalogue.ALL.forEach {
            assertTrue(it.id, it.target > 0)
            assertTrue(it.id, it.xpReward > 0)
        }
    }

    @Test
    fun `weekend walker counts steps, not walks`() {
        val walker = ChallengeCatalogue.byId(ChallengeCatalogue.WEEKEND_WALKER)!!
        assertEquals(15000, walker.target)
        assertEquals("steps", walker.unit)
    }

    @Test
    fun `byId returns null for an unknown id`() {
        assertNull(ChallengeCatalogue.byId("nope"))
    }

    @Test
    fun `each metric reads its own input`() {
        assertEquals(
            mapOf("c1" to 4000, "c2" to 3, "c3" to 9000, "c4" to 21000, "c5" to 1500),
            ChallengeCatalogue.progressById(inputs)
        )
    }

    @Test
    fun `progress never goes above the target or below 0`() {
        val big = inputs.copy(todaySteps = 25000, stepsBeforeNoon = 5000)
        assertEquals(10000, ChallengeCatalogue.progressById(big)["c1"])
        assertEquals(2000, ChallengeCatalogue.progressById(big)["c5"])
        assertEquals(0, ChallengeCatalogue.progressById(inputs.copy(streak = -1))["c2"])
    }

    @Test
    fun `weekend steps count only on Saturday and Sunday`() {
        val friday = LocalDate.of(2026, 11, 27)
        val saturday = friday.plusDays(1)
        val sunday = friday.plusDays(2)
        assertEquals(0, ChallengeProgressInputs.forToday(friday, 5000, 0, 5000).weekendSteps)
        assertEquals(5000, ChallengeProgressInputs.forToday(saturday, 5000, 0, 5000).weekendSteps)
        assertEquals(5000, ChallengeProgressInputs.forToday(sunday, 5000, 0, 5000).weekendSteps)
    }
}

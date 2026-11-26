package com.steppy.app.platform.debug

import com.steppy.app.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class DebugFlagsTest {

    /** Unit tests run on the debug build, so the helpers must be on here and nowhere else. */
    @Test
    fun demoHelpers_followTheBuildType() {
        assertEquals(BuildConfig.DEBUG, DebugFlags.SIMULATE_STEP_SENSOR_WHEN_MISSING)
        assertEquals(BuildConfig.DEBUG, DebugFlags.SHOW_TEST_REMINDER)
    }
}

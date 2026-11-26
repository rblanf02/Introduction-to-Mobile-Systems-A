package com.steppy.app.platform.debug

import com.steppy.app.BuildConfig

/**
 * Demo helpers, on in debug builds only. A release build never shows simulated steps
 * or test buttons, so its data is always real.
 */
object DebugFlags {

    /** On a phone without a step sensor (e.g. the emulator), Today offers "+500 steps (SIMULATED)". */
    val SIMULATE_STEP_SENSOR_WHEN_MISSING: Boolean = BuildConfig.DEBUG

    /** Profile shows "Send a test reminder", so the demo needn't wait until 19:00. */
    val SHOW_TEST_REMINDER: Boolean = BuildConfig.DEBUG
}

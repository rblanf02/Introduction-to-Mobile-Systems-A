package com.steppy.app.platform.sensor

/**
 * Every possible state of step tracking:
 *
 * It is a sealed interface, so these four cases are the only ones that can exist.
 * When the UI handles it with `when (state)`, the compiler forces it to cover every
 * case, so the fallback paths (no sensor, permission denied) can never be forgotten.
 */

sealed interface StepTrackingState{
    /** The device has no step counter sensor (e.g. the emulator). The UI shows manual step entry. */
    data object Unavailable : StepTrackingState

    /**
     * The user denied ACTIVITY_RECOGNITION. If [permanently] is true, Android will not show
     * the dialog again, so the UI offers an "Open settings" button. Manual entry is available meanwhile.
     */
    data class PermissionDenied (val permanently:Boolean) : StepTrackingState

    /** The sensor is working. [todaySteps] are the steps counted since midnight. */
    data class Tracking (val todaySteps:Int) : StepTrackingState

}



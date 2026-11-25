package com.steppy.app.domain

/** The main goal picked in Setup. It only changes the order of suggested challenges, never the rules. */
enum class MainMotivation(val label: String) {
    MOVE_MORE("Move more"),
    BUILD_STREAK("Build a streak"),
    STAY_HEALTHY("Stay healthy")
}

enum class ActivityLevel(val label: String) {
    JUST_STARTING("Just starting"),
    SOMEWHAT_ACTIVE("Somewhat active"),
    VERY_ACTIVE("Very active")
}

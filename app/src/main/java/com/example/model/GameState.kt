package com.example.model

enum class MascotState {
    IDLE,
    WALK,
    RUN,
    CROUCH,
    PANIC,
    GLITCH,
    DEAD
}

enum class RunPhase {
    NOT_STARTED,     // Day has started, waiting for first study block within 2-hour window
    STUDY_ACTIVE,    // Inside an active 15+ min focus block, mascot traverses
    ON_BREAK,        // Between blocks, mascot idles
    LOCKED_OUT,      // Missed 2-hour day launch window; day locked
    BREACH_KILLED,   // Wireframe kill event from blacklisted app or doomscroll
    DAY_COMPLETED    // 8:00:00 reached, escaped through fire exit
}

data class DayStats(
    val currentLevel: Int = 0,
    val streakDays: Int = 1,
    val almondWaterCans: Int = 3,
    val shameBreaches: Int = 0,
    val bankedSeconds: Long = 0L,         // Total verified study seconds banked today
    val currentBlockSeconds: Long = 0L,   // Seconds in active block (must hit 900s to bank)
    val dayLaunchSecondsLeft: Long = 7200L, // 2-hour window countdown
    val doomscrollBudgetRemainingSec: Long = 2700L, // 45 min default
    val isBlacklistBreachGraceActive: Boolean = false,
    val graceSecondsRemaining: Float = 3.0f,
    val runPhase: RunPhase = RunPhase.NOT_STARTED,
    val mascotState: MascotState = MascotState.IDLE,
    val killMessage: String = ""
)

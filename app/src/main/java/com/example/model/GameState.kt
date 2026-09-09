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
    NOT_STARTED,
    STUDY_ACTIVE,
    ON_BREAK,
    LOCKED_OUT,
    BREACH_KILLED,
    DAY_COMPLETED,
    DAY_OVER
}

data class DayStats(
    val currentLevel: Int = 0,
    val streakDays: Int = 0,
    val almondWaterCans: Int = 3,
    val shameBreaches: Int = 0,
    val bankedSeconds: Long = 0L,
    val currentBlockSeconds: Long = 0L,
    val dayLaunchSecondsLeft: Long = 7200L,
    val doomscrollBudgetRemainingSec: Long = 2700L,
    val isBlacklistBreachGraceActive: Boolean = false,
    val graceSecondsRemaining: Float = 3.0f,
    val runPhase: RunPhase = RunPhase.NOT_STARTED,
    val mascotState: MascotState = MascotState.IDLE,
    val killMessage: String = ""
)

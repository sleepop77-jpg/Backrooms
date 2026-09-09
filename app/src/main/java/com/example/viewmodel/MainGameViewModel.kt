package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.DayStats
import com.example.model.MascotState
import com.example.model.RunPhase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainGameViewModel : ViewModel() {

    private val _dayStats = MutableStateFlow(DayStats())
    val dayStats: StateFlow<DayStats> = _dayStats.asStateFlow()

    private var studyTimerJob: Job? = null
    private var graceTimerJob: Job? = null

    init {
        // Start day-launch window countdown (2 hours = 7200s)
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _dayStats.update { current ->
                    if (current.runPhase == RunPhase.NOT_STARTED && current.dayLaunchSecondsLeft > 0) {
                        val nextLeft = current.dayLaunchSecondsLeft - 1
                        if (nextLeft <= 0) {
                            current.copy(
                                dayLaunchSecondsLeft = 0,
                                runPhase = RunPhase.LOCKED_OUT,
                                mascotState = MascotState.IDLE,
                                killMessage = "RUN LOCKED: Missed the 2-hour day launch window. No progress possible today."
                            )
                        } else {
                            current.copy(dayLaunchSecondsLeft = nextLeft)
                        }
                    } else {
                        current
                    }
                }
            }
        }
    }

    // Toggle study block (Hardcore Rule: Must hit 15 mins (900s) or block is voided)
    fun toggleStudyBlock() {
        val current = _dayStats.value
        if (current.runPhase == RunPhase.LOCKED_OUT || current.runPhase == RunPhase.BREACH_KILLED) {
            return
        }

        if (current.runPhase == RunPhase.STUDY_ACTIVE) {
            // Ending block: check 15-minute threshold (900s)
            val blockSec = current.currentBlockSeconds
            val bankedGained = if (blockSec >= 900L) blockSec else 0L
            val totalBanked = current.bankedSeconds + bankedGained

            studyTimerJob?.cancel()
            _dayStats.update {
                it.copy(
                    bankedSeconds = totalBanked,
                    currentBlockSeconds = 0L,
                    runPhase = if (totalBanked >= 28800L) RunPhase.DAY_COMPLETED else RunPhase.ON_BREAK,
                    mascotState = if (totalBanked >= 28800L) MascotState.RUN else MascotState.IDLE
                )
            }
        } else {
            // Start study block
            _dayStats.update {
                it.copy(
                    runPhase = RunPhase.STUDY_ACTIVE,
                    mascotState = MascotState.WALK
                )
            }
            startBlockTimer()
        }
    }

    private fun startBlockTimer() {
        studyTimerJob?.cancel()
        studyTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _dayStats.update { current ->
                    if (current.runPhase != RunPhase.STUDY_ACTIVE) current
                    else {
                        val nextBlockSec = current.currentBlockSeconds + 1
                        val totalEstimated = current.bankedSeconds + nextBlockSec
                        val nextState = when {
                            totalEstimated >= 28800L -> MascotState.RUN // Sprinting to exit
                            nextBlockSec >= 900L -> MascotState.WALK
                            else -> MascotState.WALK
                        }

                        if (totalEstimated >= 28800L) {
                            current.copy(
                                currentBlockSeconds = 0L,
                                bankedSeconds = 28800L,
                                currentLevel = current.currentLevel + 1,
                                runPhase = RunPhase.DAY_COMPLETED,
                                mascotState = MascotState.RUN
                            )
                        } else {
                            current.copy(
                                currentBlockSeconds = nextBlockSec,
                                mascotState = nextState
                            )
                        }
                    }
                }
            }
        }
    }

    // Trigger Breach (Simulate or Detect Opening Blacklisted App)
    fun triggerBreachAlert() {
        val current = _dayStats.value
        if (current.runPhase != RunPhase.STUDY_ACTIVE) return

        graceTimerJob?.cancel()
        _dayStats.update {
            it.copy(
                isBlacklistBreachGraceActive = true,
                graceSecondsRemaining = 3.0f,
                mascotState = MascotState.PANIC
            )
        }

        graceTimerJob = viewModelScope.launch {
            var left = 3.0f
            while (left > 0f) {
                delay(200L)
                left -= 0.2f
                _dayStats.update { it.copy(graceSecondsRemaining = left) }
            }
            // Grace expired: Execute Kill Sequence
            executeKill("FATAL BREACH: Blacklisted app accessed during active study block.")
        }
    }

    // User returns to app within grace window
    fun resolveGraceSaved() {
        graceTimerJob?.cancel()
        _dayStats.update {
            it.copy(
                isBlacklistBreachGraceActive = false,
                graceSecondsRemaining = 3.0f,
                mascotState = MascotState.WALK
            )
        }
    }

    // Execute The Wireframe Kill Sequence (Loss of banked time + Level regression)
    fun executeKill(reason: String) {
        studyTimerJob?.cancel()
        graceTimerJob?.cancel()

        _dayStats.update { current ->
            val newLevel = (current.currentLevel - 1).coerceAtLeast(0)
            current.copy(
                currentLevel = newLevel,
                bankedSeconds = 0L,
                currentBlockSeconds = 0L,
                shameBreaches = current.shameBreaches + 1,
                runPhase = RunPhase.BREACH_KILLED,
                mascotState = MascotState.GLITCH,
                isBlacklistBreachGraceActive = false,
                killMessage = "$reason\nAll banked time voided.\nLevel penalty: ${current.currentLevel} -> $newLevel"
            )
        }
    }

    // Add simulated hours to test 8-hour traversal & exit door
    fun fastForwardHour(hoursToAdd: Long = 1L) {
        _dayStats.update { current ->
            val newBanked = (current.bankedSeconds + hoursToAdd * 3600L).coerceAtMost(28800L)
            val isComplete = newBanked >= 28800L
            current.copy(
                bankedSeconds = newBanked,
                runPhase = if (isComplete) RunPhase.DAY_COMPLETED else current.runPhase,
                mascotState = if (isComplete) MascotState.RUN else current.mascotState
            )
        }
    }

    // Reset Day Run
    fun resetRun() {
        studyTimerJob?.cancel()
        graceTimerJob?.cancel()
        _dayStats.update {
            it.copy(
                bankedSeconds = 0L,
                currentBlockSeconds = 0L,
                dayLaunchSecondsLeft = 7200L,
                doomscrollBudgetRemainingSec = 2700L,
                runPhase = RunPhase.NOT_STARTED,
                mascotState = MascotState.IDLE,
                isBlacklistBreachGraceActive = false,
                killMessage = ""
            )
        }
    }
}

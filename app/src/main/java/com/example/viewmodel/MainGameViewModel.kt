package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppSettings
import com.example.data.DayClock
import com.example.data.DayRecord
import com.example.data.SettingsStore
import com.example.data.StudyDatabase
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

class MainGameViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val dao = StudyDatabase.get(appContext).dayRecordDao()
    private val settingsStore = SettingsStore(appContext)

    private val _dayStats = MutableStateFlow(DayStats())
    val dayStats: StateFlow<DayStats> = _dayStats.asStateFlow()

    val settingsFlow = settingsStore.settings

    private var settings: AppSettings = AppSettings()
    private var todayKey: String = ""
    private var studyTimerJob: Job? = null
    private var graceTimerJob: Job? = null

    init {
        viewModelScope.launch {
            settings = settingsStore.snapshotSettings()
            bootstrapDay()
            startMasterTicker()
        }
    }

    private suspend fun bootstrapDay() {
        val now = System.currentTimeMillis()
        todayKey = DayClock.dateKey(now)
        val profile = settingsStore.snapshotProfile()
        var record = dao.getByDate(todayKey)

        if (profile.todayDate != todayKey) {
            settingsStore.saveProfile(profile.copy(todayDate = todayKey, todayBanked = 0L))
            val fresh = DayRecord(dateKey = todayKey, bankedSeconds = 0L, outcome = null, levelAfter = profile.level)
            dao.upsert(fresh)
            record = fresh
        } else if (record == null) {
            val fresh = DayRecord(dateKey = todayKey, bankedSeconds = profile.todayBanked, outcome = null, levelAfter = profile.level)
            dao.upsert(fresh)
            record = fresh
        }

        val banked = if (record.outcome == null) record.bankedSeconds else 0L
        val phase = when (record.outcome) {
            "COMPLETE" -> RunPhase.DAY_COMPLETED
            "KILLED" -> RunPhase.BREACH_KILLED
            "LOCKED_OUT" -> RunPhase.LOCKED_OUT
            "INCOMPLETE" -> RunPhase.DAY_OVER
            else -> RunPhase.NOT_STARTED
        }
        val windowLeft = DayClock.windowSecondsLeft(now, settings.dayStartMinutes).coerceAtLeast(0L)
        var resolvedPhase = phase
        var note = record.note
        if (phase == RunPhase.NOT_STARTED && windowLeft <= 0L) {
            note = "RUN LOCKED: Missed the 2-hour day launch window. No progress possible today."
            dao.upsert(record.copy(outcome = "LOCKED_OUT", note = note))
            resolvedPhase = RunPhase.LOCKED_OUT
        }

        _dayStats.update {
            DayStats(
                currentLevel = profile.level,
                streakDays = profile.streak,
                shameBreaches = profile.shame,
                bankedSeconds = banked,
                currentBlockSeconds = 0L,
                dayLaunchSecondsLeft = windowLeft,
                doomscrollBudgetRemainingSec = settings.budgetMinutes * 60L,
                runPhase = resolvedPhase,
                mascotState = mascotFor(resolvedPhase),
                killMessage = note
            )
        }
    }

    private fun mascotFor(phase: RunPhase): MascotState = when (phase) {
        RunPhase.STUDY_ACTIVE -> MascotState.WALK
        RunPhase.DAY_COMPLETED -> MascotState.RUN
        RunPhase.BREACH_KILLED -> MascotState.GLITCH
        else -> MascotState.IDLE
    }

    private fun startMasterTicker() {
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                tick()
            }
        }
    }

    private suspend fun tick() {
        val now = System.currentTimeMillis()
        if (DayClock.dateKey(now) != todayKey) {
            bootstrapDay()
            return
        }
        when (_dayStats.value.runPhase) {
            RunPhase.NOT_STARTED -> {
                val windowLeft = DayClock.windowSecondsLeft(now, settings.dayStartMinutes).coerceAtLeast(0L)
                _dayStats.update { it.copy(dayLaunchSecondsLeft = windowLeft) }
                if (windowLeft <= 0L) {
                    val note = "RUN LOCKED: Missed the 2-hour day launch window. No progress possible today."
                    dao.upsert(DayRecord(dateKey = todayKey, bankedSeconds = 0L, outcome = "LOCKED_OUT", levelAfter = _dayStats.value.currentLevel, note = note))
                    _dayStats.update { it.copy(runPhase = RunPhase.LOCKED_OUT, mascotState = MascotState.IDLE, killMessage = note) }
                }
            }
            RunPhase.STUDY_ACTIVE, RunPhase.ON_BREAK -> {
                if (DayClock.bedtimeSecondsLeft(now, settings.bedtimeMinutes) <= 0L) {
                    endDayIncomplete()
                }
            }
            else -> Unit
        }
    }

    fun toggleStudyBlock() {
        val current = _dayStats.value
        when (current.runPhase) {
            RunPhase.STUDY_ACTIVE -> {
                studyTimerJob?.cancel()
                val blockSec = current.currentBlockSeconds
                val gained = if (blockSec >= 900L) blockSec else 0L
                val total = (current.bankedSeconds + gained).coerceAtMost(28800L)
                if (total >= 28800L) {
                    completeDay()
                } else {
                    _dayStats.update { it.copy(bankedSeconds = total, currentBlockSeconds = 0L, runPhase = RunPhase.ON_BREAK, mascotState = MascotState.IDLE) }
                    persistBanked(total)
                }
            }
            RunPhase.NOT_STARTED, RunPhase.ON_BREAK -> {
                _dayStats.update { it.copy(runPhase = RunPhase.STUDY_ACTIVE, mascotState = MascotState.WALK) }
                startBlockTimer()
            }
            else -> Unit
        }
    }

    private fun startBlockTimer() {
        studyTimerJob?.cancel()
        studyTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val cur = _dayStats.value
                if (cur.runPhase != RunPhase.STUDY_ACTIVE) continue
                val nextBlock = cur.currentBlockSeconds + 1
                val total = cur.bankedSeconds + nextBlock
                if (total >= 28800L) {
                    completeDay()
                    return@launch
                }
                _dayStats.update { it.copy(currentBlockSeconds = nextBlock, mascotState = MascotState.WALK) }
            }
        }
    }

    private fun persistBanked(totalBanked: Long) {
        viewModelScope.launch {
            val profile = settingsStore.snapshotProfile()
            settingsStore.saveProfile(profile.copy(todayBanked = totalBanked))
            dao.getByDate(todayKey)?.let { dao.upsert(it.copy(bankedSeconds = totalBanked)) }
        }
    }

    private fun completeDay() {
        studyTimerJob?.cancel()
        graceTimerJob?.cancel()
        viewModelScope.launch {
            val profile = settingsStore.snapshotProfile()
            val newLevel = profile.level + 1
            val newStreak = profile.streak + 1
            settingsStore.saveProfile(profile.copy(level = newLevel, streak = newStreak, todayBanked = 28800L))
            dao.upsert(DayRecord(dateKey = todayKey, bankedSeconds = 28800L, outcome = "COMPLETE", levelAfter = newLevel, note = "Fire exit reached. Level up."))
            _dayStats.update {
                it.copy(
                    bankedSeconds = 28800L,
                    currentBlockSeconds = 0L,
                    currentLevel = newLevel,
                    streakDays = newStreak,
                    runPhase = RunPhase.DAY_COMPLETED,
                    mascotState = MascotState.RUN,
                    killMessage = ""
                )
            }
        }
    }

    private suspend fun endDayIncomplete() {
        val cur = _dayStats.value
        studyTimerJob?.cancel()
        graceTimerJob?.cancel()
        val bankedBlock = if (cur.runPhase == RunPhase.STUDY_ACTIVE && cur.currentBlockSeconds >= 900L) cur.currentBlockSeconds else 0L
        val total = (cur.bankedSeconds + bankedBlock).coerceAtMost(28800L)
        val profile = settingsStore.snapshotProfile()
        val note = "Bedtime reached before 8:00:00. Day incomplete: level unchanged."
        settingsStore.saveProfile(profile.copy(todayBanked = total))
        dao.upsert(DayRecord(dateKey = todayKey, bankedSeconds = total, outcome = "INCOMPLETE", levelAfter = profile.level, note = note))
        _dayStats.update {
            it.copy(
                bankedSeconds = total,
                currentBlockSeconds = 0L,
                runPhase = RunPhase.DAY_OVER,
                mascotState = MascotState.IDLE,
                isBlacklistBreachGraceActive = false,
                killMessage = note
            )
        }
    }

    fun triggerBreachAlert() {
        val current = _dayStats.value
        if (current.runPhase != RunPhase.STUDY_ACTIVE) return
        graceTimerJob?.cancel()
        val graceMs = settings.graceMillis.toLong()
        _dayStats.update { it.copy(isBlacklistBreachGraceActive = true, graceSecondsRemaining = graceMs / 1000f, mascotState = MascotState.PANIC) }
        graceTimerJob = viewModelScope.launch {
            var leftMs = graceMs
            while (leftMs > 0L) {
                delay(100L)
                leftMs -= 100L
                _dayStats.update { it.copy(graceSecondsRemaining = (leftMs / 1000f).coerceAtLeast(0f)) }
            }
            executeKill("FATAL BREACH: Blacklisted app accessed during active study block.")
        }
    }

    fun resolveGraceSaved() {
        graceTimerJob?.cancel()
        _dayStats.update { it.copy(isBlacklistBreachGraceActive = false, graceSecondsRemaining = settings.graceMillis / 1000f, mascotState = MascotState.WALK) }
    }

    fun executeKill(reason: String) {
        studyTimerJob?.cancel()
        graceTimerJob?.cancel()
        viewModelScope.launch {
            val profile = settingsStore.snapshotProfile()
            val newLevel = (profile.level - 1).coerceAtLeast(0)
            settingsStore.saveProfile(profile.copy(level = newLevel, streak = 0, shame = profile.shame + 1, todayBanked = 0L))
            dao.upsert(DayRecord(dateKey = todayKey, bankedSeconds = 0L, outcome = "KILLED", levelAfter = newLevel, note = reason))
            _dayStats.update { current ->
                current.copy(
                    currentLevel = newLevel,
                    streakDays = 0,
                    shameBreaches = profile.shame + 1,
                    bankedSeconds = 0L,
                    currentBlockSeconds = 0L,
                    runPhase = RunPhase.BREACH_KILLED,
                    mascotState = MascotState.GLITCH,
                    isBlacklistBreachGraceActive = false,
                    killMessage = reason + "\nAll banked time voided.\nLevel penalty: " + profile.level + " -> " + newLevel
                )
            }
        }
    }

    fun fastForwardHour(hoursToAdd: Long = 1L) {
        if (!BuildConfig.DEBUG) return
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

    fun resetRun() {
        val phase = _dayStats.value.runPhase
        if (phase == RunPhase.BREACH_KILLED) {
            _dayStats.update { it.copy(runPhase = RunPhase.DAY_OVER, mascotState = MascotState.IDLE) }
            return
        }
        if (phase == RunPhase.DAY_COMPLETED || phase == RunPhase.LOCKED_OUT || phase == RunPhase.DAY_OVER) return
        st
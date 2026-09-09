package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "study_os_settings")

object PrefKeys {
    val LEVEL = intPreferencesKey("level")
    val STREAK = intPreferencesKey("streak_days")
    val SHAME = intPreferencesKey("shame_breaches")
    val TODAY_DATE = stringPreferencesKey("today_date")
    val TODAY_BANKED = longPreferencesKey("today_banked_seconds")
    val DAY_START_MIN = intPreferencesKey("day_start_minutes")
    val BEDTIME_MIN = intPreferencesKey("bedtime_minutes")
    val BUDGET_MIN = intPreferencesKey("doomscroll_budget_minutes")
    val GRACE_MS = intPreferencesKey("grace_millis")
    val BLACKLIST = stringSetPreferencesKey("blacklist_packages")
}

data class AppSettings(
    val dayStartMinutes: Int = 420,
    val bedtimeMinutes: Int = 1380,
    val budgetMinutes: Int = 45,
    val graceMillis: Int = 3000,
    val blacklist: Set<String> = DEFAULT_BLACKLIST
) {
    companion object {
        val DEFAULT_BLACKLIST = setOf(
            "com.google.android.youtube",
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.snapchat.android",
            "com.reddit.frontpage",
            "com.twitter.android",
            "com.facebook.katana"
        )
    }
}

class SettingsStore(private val context: Context) {

    data class Profile(
        val level: Int = 0,
        val streak: Int = 0,
        val shame: Int = 0,
        val todayDate: String = "",
        val todayBanked: Long = 0L
    )

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            dayStartMinutes = p[PrefKeys.DAY_START_MIN] ?: 420,
            bedtimeMinutes = p[PrefKeys.BEDTIME_MIN] ?: 1380,
            budgetMinutes = p[PrefKeys.BUDGET_MIN] ?: 45,
            graceMillis = p[PrefKeys.GRACE_MS] ?: 3000,
            blacklist = p[PrefKeys.BLACKLIST] ?: AppSettings.DEFAULT_BLACKLIST
        )
    }

    val profile: Flow<Profile> = context.dataStore.data.map { p ->
        Profile(
            level = p[PrefKeys.LEVEL] ?: 0,
            streak = p[PrefKeys.STREAK] ?: 0,
            shame = p[PrefKeys.SHAME] ?: 0,
            todayDate = p[PrefKeys.TODAY_DATE] ?: "",
            todayBanked = p[PrefKeys.TODAY_BANKED] ?: 0L
        )
    }

    suspend fun snapshotSettings(): AppSettings = settings.first()

    suspend fun snapshotProfile(): Profile = profile.first()

    suspend fun saveProfile(p: Profile) {
        context.dataStore.edit { d ->
            d[PrefKeys.LEVEL] = p.level
            d[PrefKeys.STREAK] = p.streak
            d[PrefKeys.SHAME] = p.shame
            d[PrefKeys.TODAY_DATE] = p.todayDate
            d[PrefKeys.TODAY_BANKED] = p.todayBanked
        }
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val next = transform(snapshotSettings())
        context.dataStore.edit { d ->
            d[PrefKeys.DAY_START_MIN] = next.dayStartMinutes
            d[PrefKeys.BEDTIME_MIN] = next.bedtimeMinutes
            d[PrefKeys.BUDGET_MIN] = next.budgetMinutes
            d[PrefKeys.GRACE_MS] = next.graceMillis
            d[PrefKeys.BLACKLIST] = next.blacklist
        }
    }
}

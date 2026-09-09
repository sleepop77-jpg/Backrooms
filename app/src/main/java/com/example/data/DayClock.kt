package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DayClock {
    private val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun dateKey(millis: Long = System.currentTimeMillis()): String = keyFormat.format(Date(millis))

    fun epochAtMinutes(millis: Long, minutesOfDay: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
            set(Calendar.MINUTE, minutesOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun windowSecondsLeft(now: Long, dayStartMinutes: Int): Long =
        (epochAtMinutes(now, dayStartMinutes) + 2L * 3600000L - now) / 1000L

    fun bedtimeSecondsLeft(now: Long, bedtimeMinutes: Int): Long =
        (epochAtMinutes(now, bedtimeMinutes) - now) / 1000L
}

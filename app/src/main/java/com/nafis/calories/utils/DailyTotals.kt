package com.nafis.calories.utils

import java.util.Calendar
import java.util.TimeZone

data class DayTotal(
    /** Local midnight of the day, in epoch millis. */
    val dayStartMillis: Long,
    val energyKcal: Double,
    val entryCount: Int
)

/** Groups logged meals into calendar days in the device time zone. */
object DailyTotals {

    fun startOfDay(timestamp: Long, timeZone: TimeZone = TimeZone.getDefault()): Long =
        Calendar.getInstance(timeZone).apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun lastDays(
        entries: List<Pair<Long, Double>>,
        days: Int = 7,
        now: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<DayTotal> {
        val byDay = entries.groupBy { startOfDay(it.first, timeZone) }
        val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = startOfDay(now, timeZone) }
        // Step back by calendar days (not 24h) so DST changes cannot skip or repeat a day.
        calendar.add(Calendar.DAY_OF_YEAR, -(days - 1))
        return List(days) {
            val dayStart = calendar.timeInMillis
            val dayEntries = byDay[dayStart].orEmpty()
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            DayTotal(
                dayStartMillis = dayStart,
                energyKcal = dayEntries.sumOf { it.second },
                entryCount = dayEntries.size
            )
        }
    }

    /** Average over days that have at least one entry; null when none do. */
    fun averageOfLoggedDays(totals: List<DayTotal>): Double? {
        val logged = totals.filter { it.entryCount > 0 }
        return if (logged.isEmpty()) null else logged.sumOf { it.energyKcal } / logged.size
    }
}
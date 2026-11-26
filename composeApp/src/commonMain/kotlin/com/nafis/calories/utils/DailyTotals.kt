package com.nafis.calories.utils

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

data class DayTotal(
    val dayStartMillis: Long,
    val energyKcal: Double,
    val entryCount: Int
)

object DailyTotals {

    fun startOfDay(timestamp: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): Long =
        Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone).date
            .atStartOfDayIn(timeZone)
            .toEpochMilliseconds()

    fun lastDays(
        entries: List<Pair<Long, Double>>,
        days: Int = 7,
        now: Long = Clock.System.now().toEpochMilliseconds(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): List<DayTotal> {
        val byDay = entries.groupBy { startOfDay(it.first, timeZone) }
        val today = Instant.fromEpochMilliseconds(now).toLocalDateTime(timeZone).date
        val firstDay = today.minus(DatePeriod(days = days - 1))
        return List(days) { offset ->
            val dayStart = firstDay.plus(DatePeriod(days = offset)).atStartOfDayIn(timeZone).toEpochMilliseconds()
            val dayEntries = byDay[dayStart].orEmpty()
            DayTotal(
                dayStartMillis = dayStart,
                energyKcal = dayEntries.sumOf { it.second },
                entryCount = dayEntries.size
            )
        }
    }

    fun averageOfLoggedDays(totals: List<DayTotal>): Double? {
        val logged = totals.filter { it.entryCount > 0 }
        return if (logged.isEmpty()) null else logged.sumOf { it.energyKcal } / logged.size
    }
}
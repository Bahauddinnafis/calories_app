package com.nafis.calories.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DailyTotalsTest {

    private val zone = TimeZone.of("Asia/Jakarta")

    private fun at(day: Int, hour: Int): Long =
        LocalDateTime(2026, 11, day, hour, 0).toInstant(zone).toEpochMilliseconds()

    @Test
    fun returnsSevenDaysOldestFirstIncludingEmptyDays() {
        val totals = DailyTotals.lastDays(
            entries = listOf(at(23, 8) to 300.0, at(23, 19) to 500.0, at(20, 12) to 700.0),
            now = at(23, 21),
            timeZone = zone
        )

        assertEquals(7, totals.size)
        assertEquals(DailyTotals.startOfDay(at(17, 0), zone), totals.first().dayStartMillis)
        assertEquals(800.0, totals.last().energyKcal)
        assertEquals(2, totals.last().entryCount)
        assertEquals(700.0, totals[3].energyKcal)
        assertEquals(0, totals[4].entryCount)
    }

    @Test
    fun earlyMorningMealsCountForTheLocalDay() {
        assertEquals(at(23, 0), DailyTotals.startOfDay(at(23, 2), zone))
    }

    @Test
    fun entriesOlderThanTheWindowAreIgnored() {
        val totals = DailyTotals.lastDays(listOf(at(1, 12) to 999.0), now = at(23, 12), timeZone = zone)

        assertEquals(0.0, totals.sumOf { it.energyKcal })
    }

    @Test
    fun averageSkipsDaysWithoutEntries() {
        val totals = DailyTotals.lastDays(
            entries = listOf(at(23, 8) to 1800.0, at(21, 8) to 2200.0),
            now = at(23, 21),
            timeZone = zone
        )

        assertEquals(2000.0, DailyTotals.averageOfLoggedDays(totals))
    }

    @Test
    fun averageIsNullForAnEmptyWeek() {
        assertNull(DailyTotals.averageOfLoggedDays(DailyTotals.lastDays(emptyList(), now = at(23, 12), timeZone = zone)))
    }
}

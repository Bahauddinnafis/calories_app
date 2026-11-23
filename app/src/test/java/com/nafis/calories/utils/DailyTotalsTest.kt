package com.nafis.calories.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DailyTotalsTest {

    private val zone = TimeZone.getTimeZone("Asia/Jakarta")

    private fun at(day: Int, hour: Int): Long = Calendar.getInstance(zone).apply {
        clear()
        set(2026, Calendar.NOVEMBER, day, hour, 0)
    }.timeInMillis

    @Test
    fun `returns seven days oldest first including empty days`() {
        val totals = DailyTotals.lastDays(
            entries = listOf(at(23, 8) to 300.0, at(23, 19) to 500.0, at(20, 12) to 700.0),
            now = at(23, 21),
            timeZone = zone
        )
        assertEquals(7, totals.size)
        assertEquals(DailyTotals.startOfDay(at(17, 0), zone), totals.first().dayStartMillis)
        assertEquals(800.0, totals.last().energyKcal, 0.0)
        assertEquals(2, totals.last().entryCount)
        assertEquals(700.0, totals[3].energyKcal, 0.0)
        assertEquals(0, totals[4].entryCount)
    }

    @Test
    fun `entries older than the window are ignored`() {
        val totals = DailyTotals.lastDays(listOf(at(1, 12) to 999.0), now = at(23, 12), timeZone = zone)
        assertEquals(0.0, totals.sumOf { it.energyKcal }, 0.0)
    }

    @Test
    fun `average skips days without entries`() {
        val totals = DailyTotals.lastDays(
            entries = listOf(at(23, 8) to 1800.0, at(21, 8) to 2200.0),
            now = at(23, 21),
            timeZone = zone
        )
        assertEquals(2000.0, DailyTotals.averageOfLoggedDays(totals)!!, 0.0)
    }

    @Test
    fun `average is null for an empty week`() {
        assertNull(DailyTotals.averageOfLoggedDays(DailyTotals.lastDays(emptyList(), now = at(23, 12), timeZone = zone)))
    }
}

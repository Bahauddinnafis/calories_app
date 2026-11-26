package com.nafis.calories.utils

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** Indonesian date labels in the device time zone, matching the original `id-ID` patterns. */
object DateFormat {

    private val dayNames = mapOf(
        DayOfWeek.MONDAY to "Senin",
        DayOfWeek.TUESDAY to "Selasa",
        DayOfWeek.WEDNESDAY to "Rabu",
        DayOfWeek.THURSDAY to "Kamis",
        DayOfWeek.FRIDAY to "Jumat",
        DayOfWeek.SATURDAY to "Sabtu",
        DayOfWeek.SUNDAY to "Minggu"
    )

    private val monthNames = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    /** `EEE`, e.g. "Sen". */
    fun shortWeekday(epochMillis: Long): String = dayName(local(epochMillis)).take(3)

    /** `EEEE, d MMMM`, e.g. "Rabu, 25 November". */
    fun weekdayDayMonth(epochMillis: Long): String {
        val date = local(epochMillis)
        return "${dayName(date)}, ${date.day} ${monthNames[date.month.number - 1]}"
    }

    /** `EEEE, d MMMM yyyy`, e.g. "Rabu, 25 November 2026". */
    fun fullDate(epochMillis: Long): String = "${weekdayDayMonth(epochMillis)} ${local(epochMillis).year}"

    /** `HH:mm`. */
    fun time(epochMillis: Long): String {
        val date = local(epochMillis)
        return "${date.hour.toString().padStart(2, '0')}:${date.minute.toString().padStart(2, '0')}"
    }

    fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    fun currentHour(): Int = local(nowMillis()).hour

    private fun dayName(date: LocalDateTime): String = dayNames.getValue(date.dayOfWeek)

    private fun local(epochMillis: Long): LocalDateTime =
        Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
}
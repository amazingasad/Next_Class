package com.example.nextclass.domain.model

import java.time.LocalDate

/** A run of calendar days with no classes, for example an Eid break or exam week. Both ends included. */
data class BreakPeriod(
    val id: Long,
    val name: String,
    val start: LocalDate,
    val end: LocalDate,
) {
    val days: Int get() = (end.toEpochDay() - start.toEpochDay()).toInt() + 1
}

object Breaks {

    fun on(date: LocalDate, breaks: List<BreakPeriod>): BreakPeriod? =
        breaks.firstOrNull { !date.isBefore(it.start) && !date.isAfter(it.end) }

    /** The break that is under way today, or the nearest one that starts within [withinDays]. */
    fun current(today: LocalDate, breaks: List<BreakPeriod>, withinDays: Int = 14): BreakPeriod? =
        on(today, breaks) ?: breaks
            .filter { it.start.isAfter(today) && it.start.toEpochDay() - today.toEpochDay() <= withinDays }
            .minByOrNull { it.start }

    fun describe(item: BreakPeriod, today: LocalDate): String {
        val range = shortDate(item.start) + if (item.days > 1) " to " + shortDate(item.end) else ""
        return when {
            on(today, listOf(item)) != null -> "On now, until " + shortDate(item.end)
            item.start.isAfter(today) -> {
                val away = (item.start.toEpochDay() - today.toEpochDay()).toInt()
                (if (away == 1) "Starts tomorrow" else "Starts in $away days") + ", " + range
            }
            else -> range
        }
    }

    fun shortDate(date: LocalDate): String {
        val month = date.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
        return "$month ${date.dayOfMonth}"
    }
}

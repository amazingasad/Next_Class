package com.example.nextclass.domain.model

/** One finished focus session. */
data class StudyEntry(
    val id: Long,
    val courseKey: String,
    val courseCode: String,
    val date: String,
    val minutes: Int,
)

/** The timer that is counting, paused, or finished waiting to be logged. */
data class FocusSession(
    val courseKey: String,
    val courseCode: String,
    val totalSec: Int,
    /** Epoch millis when the timer reaches zero while running. */
    val endAt: Long,
    /** Seconds left when paused, or -1 while running. */
    val pausedLeftSec: Int,
) {
    val running: Boolean get() = pausedLeftSec < 0

    fun leftSec(nowMillis: Long): Int =
        if (running) maxOf(0, ((endAt - nowMillis + 999) / 1000).toInt()) else pausedLeftSec
}

object StudyStats {

    fun total(entries: List<StudyEntry>): Int = entries.sumOf { it.minutes }

    fun perCourse(entries: List<StudyEntry>): List<Pair<String, Int>> =
        entries.groupBy { it.courseCode }
            .map { (code, list) -> code to list.sumOf { it.minutes } }
            .sortedByDescending { it.second }
}

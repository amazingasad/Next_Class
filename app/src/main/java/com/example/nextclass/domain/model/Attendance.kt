package com.example.nextclass.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

/** One marked class: a course on a date at a start time, present or absent. */
data class AttendanceMark(
    val courseKey: String,
    val date: String,
    val startTime: String,
    val present: Boolean,
)

/** A class that has already taken place (or is under way) on a given date. */
data class Occurrence(val session: ClassSession, val date: LocalDate) {
    val courseKey: String get() = CourseCodes.key(session.courseCode)
    val id: String get() = "$courseKey|$date|${session.startTime}"
}

/** Attendance totals for one course. */
data class CourseAttendance(
    val key: String,
    val code: String,
    val title: String,
    val present: Int,
    val absent: Int,
) {
    val total: Int get() = present + absent

    /** Whole-number percentage, or null before anything is marked. */
    val percent: Int? get() = if (total == 0) null else (present * 100.0 / total).roundToInt()

    /** 0..1 for progress bars. */
    val fraction: Float get() = if (total == 0) 0f else present.toFloat() / total

    val meetsTarget: Boolean get() = total == 0 || present * 100 >= total * AttendanceMath.TARGET

    val advice: String get() = AttendanceMath.advice(present, total)
}

object AttendanceMath {
    const val TARGET = 75

    /** Classes in a row that must be attended to climb back to the target. */
    fun classesNeeded(present: Int, total: Int): Int =
        maxOf(0, (TARGET * total - 100 * present + (100 - TARGET) - 1) / (100 - TARGET))

    /** Further classes that can be missed while staying at or above the target. */
    fun canMiss(present: Int, total: Int): Int =
        maxOf(0, (100 * present - TARGET * total) / TARGET)

    fun advice(present: Int, total: Int): String {
        if (total == 0) return "Nothing marked yet."
        if (present * 100 >= total * TARGET) {
            val spare = canMiss(present, total)
            return when (spare) {
                0 -> "Right at $TARGET%. Do not miss the next class."
                1 -> "You can miss 1 more class and stay above $TARGET%."
                else -> "You can miss $spare more classes and stay above $TARGET%."
            }
        }
        val need = classesNeeded(present, total)
        return if (need == 1) {
            "Attend the next class to reach $TARGET%."
        } else {
            "Attend the next $need classes in a row to reach $TARGET%."
        }
    }

    /**
     * Every scheduled class from [days] days ago up to now. A class today counts only once it has
     * started, and classes cancelled for a date are skipped.
     */
    fun occurrences(
        sessions: List<ClassSession>,
        overrides: List<ClassOverride>,
        now: LocalDateTime,
        days: Int,
    ): List<Occurrence> {
        val today = now.toLocalDate()
        val minutesNow = now.hour * 60 + now.minute
        val result = ArrayList<Occurrence>()
        for (offset in 0 until days) {
            val date = today.minusDays(offset.toLong())
            val dateText = date.toString()
            sessions
                .filter { it.dayOfWeek == date.dayOfWeek.value }
                .sortedBy { it.startTime }
                .forEach { session ->
                    val started = date.isBefore(today) || session.startMinutes <= minutesNow
                    val cancelled = overrides.any {
                        it.sessionId == session.id && it.date == dateText && it.cancelled
                    }
                    if (started && !cancelled) result += Occurrence(session, date)
                }
        }
        return result
    }

    /** One row per course in the routine (and any course with marks), lowest percentage first. */
    fun summarize(
        sessions: List<ClassSession>,
        courses: List<Course>,
        marks: List<AttendanceMark>,
    ): List<CourseAttendance> {
        val titles = LinkedHashMap<String, Pair<String, String>>()
        courses.forEach { titles[it.key] = it.code to it.title }
        sessions.forEach { s ->
            val key = CourseCodes.key(s.courseCode)
            val known = titles[key]
            titles[key] = (known?.first ?: s.courseCode) to (known?.second?.ifBlank { s.courseTitle } ?: s.courseTitle)
        }
        val byCourse = marks.groupBy { it.courseKey }
        byCourse.keys.forEach { key ->
            if (key !in titles) titles[key] = key to ""
        }
        val routineKeys = sessions.map { CourseCodes.key(it.courseCode) }.toSet()
        return titles
            .filter { (key, _) -> key in routineKeys || byCourse[key].orEmpty().isNotEmpty() }
            .map { (key, info) ->
                val list = byCourse[key].orEmpty()
                CourseAttendance(
                    key = key,
                    code = info.first,
                    title = info.second,
                    present = list.count { it.present },
                    absent = list.count { !it.present },
                )
            }
            .sortedWith(
                compareBy<CourseAttendance> { it.percent == null }
                    .thenBy { it.percent ?: 0 }
                    .thenBy { it.code },
            )
    }
}

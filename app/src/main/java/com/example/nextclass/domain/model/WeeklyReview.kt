package com.example.nextclass.domain.model

import java.time.LocalDate

data class WeeklyReviewData(
    val from: LocalDate,
    val to: LocalDate,
    val held: Int,
    val present: Int,
    val absent: Int,
    val unmarked: Int,
    val tasksDone: Int,
    val tasksTotal: Int,
    val studyMinutes: Int,
    val studyByCourse: List<Pair<String, Int>>,
    val weakest: CourseAttendance?,
    val nextBreak: BreakPeriod?,
) {
    /** One line for the notification. */
    val summary: String
        get() {
            val parts = ArrayList<String>()
            if (held > 0) parts += "$present of $held classes attended"
            if (tasksTotal > 0) parts += "$tasksDone of $tasksTotal tasks done"
            parts += if (studyMinutes > 0) TimeFormat.duration(studyMinutes) + " studied" else "no study time logged"
            return parts.joinToString(", ") + "."
        }
}

object WeeklyReview {

    fun compute(
        end: LocalDate,
        sessions: List<ClassSession>,
        overrides: List<ClassOverride>,
        breaks: List<BreakPeriod>,
        courses: List<Course>,
        marks: List<AttendanceMark>,
        tasks: List<PlannerTask>,
        study: List<StudyEntry>,
    ): WeeklyReviewData {
        val start = end.minusDays(6)
        val occurrences = AttendanceMath.occurrences(sessions, overrides, end.atTime(23, 59), 7, breaks)
        val markById = marks.associateBy { "${it.courseKey}|${it.date}|${it.startTime}" }
        var present = 0
        var absent = 0
        occurrences.forEach { o ->
            when (markById[o.id]?.present) {
                true -> present++
                false -> absent++
                null -> Unit
            }
        }
        val inWeekTasks = tasks.filter { it.date >= start.toString() && it.date <= end.toString() }
        val inWeekStudy = study.filter { it.date >= start.toString() && it.date <= end.toString() }
        val weakest = AttendanceMath.summarize(sessions, courses, marks)
            .filter { it.total > 0 && !it.meetsTarget }
            .minByOrNull { it.percent ?: 100 }
        return WeeklyReviewData(
            from = start,
            to = end,
            held = occurrences.size,
            present = present,
            absent = absent,
            unmarked = occurrences.size - present - absent,
            tasksDone = inWeekTasks.count { it.done },
            tasksTotal = inWeekTasks.size,
            studyMinutes = StudyStats.total(inWeekStudy),
            studyByCourse = StudyStats.perCourse(inWeekStudy),
            weakest = weakest,
            nextBreak = Breaks.current(end, breaks, 21),
        )
    }
}

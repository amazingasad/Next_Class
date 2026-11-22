package com.example.nextclass.domain.model

/** Finds scanned rows the AI may have misread, so the user can check those first. */
object ScanReview {

    private const val EARLIEST_START = 7 * 60
    private const val LATEST_END = 21 * 60 + 30
    private const val SHORTEST = 30
    private const val LONGEST = 4 * 60
    private val CODE_SHAPE = Regex("^[A-Z]{2,6}\\d{2,4}[A-Z]?$")

    /** Session id to a short, plain reason. Rows with nothing wrong are absent. */
    fun issues(sessions: List<ClassSession>): Map<Long, String> {
        val result = LinkedHashMap<Long, String>()
        sessions.forEach { session ->
            val problems = ArrayList<String>()

            if (session.room.isBlank()) problems += "Room is missing"
            if (!CODE_SHAPE.matches(CourseCodes.key(session.courseCode))) problems += "Check the course code"

            val duration = session.endMinutes - session.startMinutes
            when {
                duration < SHORTEST -> problems += "Very short class"
                duration > LONGEST -> problems += "Very long class"
                session.startMinutes < EARLIEST_START || session.endMinutes > LATEST_END ->
                    problems += "Unusual time"
            }

            val sameDay = sessions.filter { it.id != session.id && it.dayOfWeek == session.dayOfWeek }
            val duplicate = sameDay.any {
                CourseCodes.key(it.courseCode) == CourseCodes.key(session.courseCode) &&
                    it.startTime == session.startTime
            }
            if (duplicate) {
                problems += "Duplicate row"
            } else {
                val clash = sameDay.firstOrNull {
                    it.startMinutes < session.endMinutes && session.startMinutes < it.endMinutes
                }
                if (clash != null) problems += "Overlaps ${clash.courseCode}"
            }

            if (problems.isNotEmpty()) result[session.id] = problems.joinToString(" • ")
        }
        return result
    }
}

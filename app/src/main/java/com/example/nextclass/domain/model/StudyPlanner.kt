package com.example.nextclass.domain.model

import java.time.LocalDate

/** One suggested study block inside a free window. */
data class StudySlot(
    val startMinutes: Int,
    val endMinutes: Int,
    val courseCode: String,
    val courseTitle: String,
    val reason: String,
) {
    val minutes: Int get() = endMinutes - startMinutes
    val range: String
        get() = TimeFormat.clock12(hhmm(startMinutes)) + " — " + TimeFormat.clock12(hhmm(endMinutes))

    fun startText(): String = TimeFormat.clock12(hhmm(startMinutes))

    fun endText(): String = TimeFormat.clock12(hhmm(endMinutes))

    /** Text for a to-do item created from this suggestion. */
    val taskTitle: String
        get() = "Study $courseCode" + (if (courseTitle.isNotBlank()) " ($courseTitle)" else "") +
            ", " + TimeFormat.clock12(hhmm(startMinutes)) + " to " + TimeFormat.clock12(hhmm(endMinutes))

    private fun hhmm(total: Int): String = "%02d:%02d".format(total / 60, total % 60)
}

data class StudyPlan(
    val offDay: Boolean,
    val freeMinutes: Int,
    val slots: List<StudySlot>,
) {
    val headline: String
        get() = when {
            offDay -> "Off day. Plenty of time to get ahead."
            slots.isEmpty() -> "No long free gaps on this day."
            else -> TimeFormat.duration(freeMinutes) + " free between classes."
        }
}

/**
 * Looks at the day's classes, finds the free windows and suggests which courses to study in them.
 * An off day gets several blocks. A class day gets one or two, in the longest gaps.
 */
object StudyPlanner {

    private const val DAY_START = 8 * 60
    private const val DAY_END = 21 * 60 + 30
    private const val MIN_WINDOW = 50
    private const val BREATHER = 10
    private const val MAX_BLOCK = 90
    private const val MIN_BLOCK = 30

    private data class Window(val start: Int, val end: Int, val hasPrev: Boolean, val hasNext: Boolean)

    private data class Block(val start: Int, val end: Int)

    fun plan(
        date: LocalDate,
        sessions: List<ClassSession>,
        overrides: List<ClassOverride>,
        courses: List<Course>,
        nowMinutes: Int?,
        breaks: List<BreakPeriod> = emptyList(),
        studyMinutesByCourse: Map<String, Int> = emptyMap(),
    ): StudyPlan {
        if (sessions.isEmpty()) return StudyPlan(offDay = false, freeMinutes = 0, slots = emptyList())

        val dateText = date.toString()
        val onBreak = Breaks.on(date, breaks) != null
        val dayClasses = if (onBreak) emptyList() else sessions
            .filter { it.dayOfWeek == date.dayOfWeek.value }
            .filter { s -> overrides.none { it.sessionId == s.id && it.date == dateText && it.cancelled } }
            .sortedBy { it.startMinutes }
        val tomorrowKeys = sessions
            .filter { it.dayOfWeek == date.plusDays(1).dayOfWeek.value }
            .map { CourseCodes.key(it.courseCode) }
            .toSet()
        val weekly = sessions.groupingBy { CourseCodes.key(it.courseCode) }.eachCount()
        val titleByKey = courses.associate { it.key to it.title }
        val pool = LinkedHashMap<String, Pair<String, String>>()
        sessions.forEach { s ->
            val key = CourseCodes.key(s.courseCode)
            if (key !in pool) pool[key] = s.courseCode to (titleByKey[key]?.ifBlank { null } ?: s.courseTitle)
        }

        val offDay = dayClasses.isEmpty()
        val blocks = if (offDay) offDayBlocks(nowMinutes) else classDayBlocks(dayClasses, nowMinutes)
        val free = blocks.sumOf { it.end - it.start }
        val used = HashSet<String>()

        fun assign(block: Block): StudySlot {
            var best: Triple<String, Double, String>? = null
            for ((key, info) in pool) {
                if (key in used) continue
                var score = 0.1 * (weekly[key] ?: 0) + ((key.hashCode() + date.toEpochDay()).mod(7)) * 0.05
                score += maxOf(0.0, 1.5 - (studyMinutesByCourse[key] ?: 0) / 120.0)
                var reason = if (offDay) "Off day, extra time to get ahead" else "No class soon, a good time to get ahead"
                val before = dayClasses.any { CourseCodes.key(it.courseCode) == key && it.endMinutes <= block.start }
                val later = dayClasses
                    .filter { CourseCodes.key(it.courseCode) == key && it.startMinutes >= block.end }
                    .minByOrNull { it.startMinutes }
                if (before) {
                    score += 4
                    reason = "Revise today's class while it is fresh"
                } else if (later != null) {
                    score += 3
                    reason = "Prepare for your " + TimeFormat.clock12(later.startTime) + " class"
                } else if (key in tomorrowKeys) {
                    score += 2
                    reason = "Class tomorrow, get ready for it"
                }
                if (best == null || score > best.second) best = Triple(key, score, reason)
            }
            if (best == null) {
                used.clear()
                return assign(block)
            }
            used += best.first
            val info = pool.getValue(best.first)
            return StudySlot(block.start, block.end, info.first, info.second, best.third)
        }

        val slots = blocks.map { assign(it) }
        return StudyPlan(offDay = offDay, freeMinutes = free, slots = slots)
    }

    private fun round5Up(value: Int): Int = (value + 4) / 5 * 5

    private fun round5Down(value: Int): Int = value / 5 * 5

    private fun offDayBlocks(nowMinutes: Int?): List<Block> {
        val raw = listOf(
            Block(9 * 60, 10 * 60 + 30),
            Block(10 * 60 + 45, 12 * 60),
            Block(14 * 60, 15 * 60 + 30),
            Block(15 * 60 + 45, 17 * 60),
            Block(19 * 60, 20 * 60 + 30),
        )
        return raw.filter { nowMinutes == null || it.start >= nowMinutes }
    }

    private fun classDayBlocks(day: List<ClassSession>, nowMinutes: Int?): List<Block> {
        val windows = ArrayList<Window>()
        var cursor = DAY_START
        var hasPrev = false
        day.forEach { s ->
            if (s.startMinutes > cursor) windows += Window(cursor, s.startMinutes, hasPrev, true)
            cursor = maxOf(cursor, s.endMinutes)
            hasPrev = true
        }
        if (DAY_END > cursor) windows += Window(cursor, DAY_END, hasPrev, false)

        val candidates = windows.mapNotNull { w ->
            var start = w.start
            var prev = w.hasPrev
            if (nowMinutes != null && nowMinutes > start) {
                start = round5Up(nowMinutes)
                prev = false
            }
            val free = w.end - start
            if (free < MIN_WINDOW) return@mapNotNull null
            val lead = if (prev) BREATHER else 0
            val tail = if (w.hasNext) BREATHER else 0
            val length = minOf(MAX_BLOCK, round5Down(free - lead - tail))
            if (length < MIN_BLOCK) return@mapNotNull null
            val blockStart = if (!prev && w.hasNext) round5Down(w.end - tail - length) else round5Up(start + lead)
            Block(blockStart, blockStart + length)
        }
        if (candidates.isEmpty()) return emptyList()
        val total = candidates.sumOf { it.end - it.start }
        val count = if (total < 150) 1 else 2
        return candidates
            .sortedByDescending { it.end - it.start }
            .take(count)
            .sortedBy { it.start }
    }
}

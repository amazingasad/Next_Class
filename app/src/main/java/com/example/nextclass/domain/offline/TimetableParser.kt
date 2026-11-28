package com.example.nextclass.domain.offline

import com.example.nextclass.domain.model.ClassSession
import com.example.nextclass.domain.model.CourseCodes
import com.example.nextclass.domain.model.SessionType
import kotlin.math.abs

/**
 * Reads a grid timetable (days across the page in blocks of numbered periods, one row group per
 * year, semester and section) from recognised words and the page pixels. It runs on any device
 * and needs no network. The words can come from a PDF text layer or from OCR.
 */
object TimetableParser {

    private val DAYS = mapOf(
        "monday" to 1, "tuesday" to 2, "wednesday" to 3, "thursday" to 4,
        "friday" to 5, "saturday" to 6, "sunday" to 7,
    )
    private val ORDINAL = Regex("^(\\d{1,2})(st|nd|rd|th)$", RegexOption.IGNORE_CASE)
    private val YEAR_LINE = Regex("^(\\d)(?:st|nd|rd|th)\\s+Year$", RegexOption.IGNORE_CASE)
    private val SEM_LINE = Regex("^(Odd|Even)\\s+Sem(?:ester)?$", RegexOption.IGNORE_CASE)
    private val SEC_LINE = Regex("^Sec(?:tion)?\\.?\\s*([A-Za-z])$", RegexOption.IGNORE_CASE)
    private val TIME = Regex("(\\d{1,2})[.:](\\d{2})\\s*(am|pm)?", RegexOption.IGNORE_CASE)
    private val CODE = Regex("^[A-Za-z]{2,5}\\s?-?\\d{3,4}[A-Za-z]?$")
    private val ROOM_WORDS = Regex("lab|seminar|hall|room|gallery|auditorium|library|studio", RegexOption.IGNORE_CASE)
    private val ROOM_NUMBER = Regex("^\\d{2,4}[A-Za-z]?$")

    private const val BOUNDARY_RUN = 0.80f

    private class Block(
        val centers: List<Float>,
        val day: Int,
        val headerBottom: Float,
        val times: List<Pair<String, String>>?,
    ) {
        val colWidth: Float get() = (centers.last() - centers.first()) / (centers.size - 1)
        val left: Float get() = centers.first() - colWidth / 2f
        val right: Float get() = centers.last() + colWidth / 2f
        fun edge(boundary: Int): Float = (centers[boundary] + centers[boundary + 1]) / 2f
    }

    private class RowBand(val group: GroupKey, val top: Float, val bottom: Float)

    fun read(pages: List<OfflinePage>): OfflineOutcome {
        val warnings = ArrayList<String>()
        val blocksByPage = pages.map { page -> findBlocks(page.words, warnings) }
        val fallbackTimes = blocksByPage.flatten().firstNotNullOfOrNull { it.times }
        if (blocksByPage.all { it.isEmpty() }) {
            return OfflineOutcome.NotATimetable("No day and period headers were found on the page.")
        }
        if (fallbackTimes == null) {
            return OfflineOutcome.NotATimetable("The period times could not be read.")
        }

        val cells = ArrayList<OfflineCell>()
        val groups = LinkedHashSet<GroupKey>()
        pages.forEachIndexed { index, page ->
            val blocks = blocksByPage[index]
            blocks.forEachIndexed { blockIndex, block ->
                val previousRight = blocks.getOrNull(blockIndex - 1)
                    ?.takeIf { abs(it.headerBottom - block.headerBottom) < 6f * rowHeight(page.words) }
                    ?.right ?: 0f
                val bands = findRows(page.words, block, previousRight)
                bands.forEach { groups += it.group }
                val times = block.times ?: fallbackTimes
                val lineMode = page.raster != null && hasAnyBoundary(page.raster, block, bands)
                for (band in bands) {
                    cells += cellsOf(page, block, band, times, lineMode, warnings)
                }
            }
        }
        if (groups.isEmpty()) {
            return OfflineOutcome.NotATimetable("No year, semester and section rows were found.")
        }
        return OfflineOutcome.Found(TimetableGrid(groups.toList(), cells, warnings.distinct()))
    }

    /** Picks the rows that match [filter]. With several candidates the caller must ask the user. */
    fun select(grid: TimetableGrid, filter: String): OfflineSelection {
        val matching = grid.groups.filter { it.matches(filter) }
        val chosen = matching.singleOrNull()
            ?: return OfflineSelection.NeedGroup(if (matching.isEmpty()) grid.groups else matching)
        return OfflineSelection.Ready(chosen, sessionsFor(grid, chosen), grid.warnings)
    }

    fun sessionsFor(grid: TimetableGrid, group: GroupKey): List<ClassSession> {
        val sessions = ArrayList<ClassSession>()
        for (cell in grid.cells.filter { it.group == group }) {
            sessions += sessionsOfCell(cell)
        }
        return merge(sessions).sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }, { it.courseCode }))
    }

    // ---- Headers -----------------------------------------------------------------------

    private fun rowHeight(words: List<OcrWord>): Float {
        val heights = words.map { it.height }.sorted()
        return if (heights.isEmpty()) 10f else heights[heights.size / 2]
    }

    private fun findBlocks(words: List<OcrWord>, warnings: MutableList<String>): List<Block> {
        val h = rowHeight(words)
        val dayWords = words.filter { DAYS.containsKey(it.text.lowercase().trim(':', ',', '.')) }
        val ordinals = words.filter { word ->
            ORDINAL.matches(word.text) && words.none { other ->
                other.text.equals("Year", true) &&
                    abs(other.centerY - word.centerY) < word.height * 0.6f &&
                    other.left >= word.right - 1f && other.left - word.right < word.height * 3f
            }
        }
        if (dayWords.isEmpty() || ordinals.size < 5) return emptyList()

        // Header rows: ordinals that share a baseline.
        val rows = ArrayList<MutableList<OcrWord>>()
        for (word in ordinals.sortedBy { it.centerY }) {
            val row = rows.lastOrNull()
            if (row != null && abs(row.first().centerY - word.centerY) < h * 0.8f) row += word else rows += mutableListOf(word)
        }

        val blocks = ArrayList<Block>()
        for (row in rows) {
            val sorted = row.sortedBy { it.centerX }
            val groups = ArrayList<MutableList<OcrWord>>()
            for (word in sorted) {
                val number = ORDINAL.matchEntire(word.text)!!.groupValues[1].toInt()
                val current = groups.lastOrNull()
                val previous = current?.lastOrNull()?.let { ORDINAL.matchEntire(it.text)!!.groupValues[1].toInt() }
                if (current == null || previous == null || number <= previous) groups += mutableListOf(word) else current += word
            }
            for (group in groups) {
                val numbers = group.map { ORDINAL.matchEntire(it.text)!!.groupValues[1].toInt() }
                if (group.size < 5 || numbers != (1..group.size).toList()) continue
                val centers = group.map { it.centerX }
                val rowTop = group.minOf { it.top }
                val rowBottom = group.maxOf { it.bottom }
                val middle = (centers.first() + centers.last()) / 2f
                val title = dayWords
                    .filter { it.centerY < rowTop }
                    .minByOrNull { abs(it.centerX - middle) + (rowTop - it.centerY) * 2f }
                if (title == null) {
                    warnings += "A block of periods had no day name above it."
                    continue
                }
                val day = DAYS.getValue(title.text.lowercase().trim(':', ',', '.'))
                val colWidth = (centers.last() - centers.first()) / (centers.size - 1)
                val timeWords = words.filter {
                    TIME.containsMatchIn(it.text) &&
                        it.centerY > rowBottom && it.centerY < rowBottom + h * 3f &&
                        it.centerX > centers.first() - colWidth / 2f && it.centerX < centers.last() + colWidth / 2f
                }.sortedBy { it.centerX }
                val times = parseTimes(timeWords.joinToString(" ") { it.text }, group.size)
                val bottom = timeWords.maxOfOrNull { it.bottom } ?: rowBottom
                blocks += Block(centers, day, bottom, times)
            }
        }
        return blocks
    }

    /** Pairs the clock times printed in the header into periods, working out am and pm from the order. */
    private fun parseTimes(text: String, periods: Int): List<Pair<String, String>>? {
        val matches = TIME.findAll(text).toList()
        if (matches.size != periods * 2) return null
        var previous = -1
        val minutes = ArrayList<Int>()
        for (match in matches) {
            val hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].toInt()
            if (hour > 23 || minute > 59) return null
            val meridiem = match.groupValues[3].lowercase()
            val value = when (meridiem) {
                "am" -> (hour % 12) * 60 + minute
                "pm" -> (hour % 12 + 12) * 60 + minute
                else -> {
                    val candidates = listOf(hour * 60 + minute, (hour % 12) * 60 + minute, (hour % 12 + 12) * 60 + minute)
                        .filter { it >= previous }
                    candidates.minOrNull() ?: return null
                }
            }
            if (value < previous) return null
            minutes += value
            previous = value
        }
        return (0 until periods).map { i -> hhmm(minutes[2 * i]) to hhmm(minutes[2 * i + 1]) }
    }

    private fun hhmm(total: Int): String = "%02d:%02d".format(total / 60, total % 60)

    // ---- Rows ----------------------------------------------------------------------------

    private fun lineClusters(words: List<OcrWord>): List<List<OcrWord>> {
        if (words.isEmpty()) return emptyList()
        val tolerance = rowHeight(words) * 0.6f
        val lines = ArrayList<MutableList<OcrWord>>()
        for (word in words.sortedBy { it.centerY }) {
            val line = lines.lastOrNull()
            if (line != null && abs(line.map { it.centerY }.average().toFloat() - word.centerY) < tolerance) line += word
            else lines += mutableListOf(word)
        }
        return lines.map { line -> line.sortedBy { it.left } }
    }

    private fun findRows(words: List<OcrWord>, block: Block, previousRight: Float): List<RowBand> {
        val region = words.filter {
            it.centerX >= previousRight && it.centerX < block.left && it.centerY > block.headerBottom
        }
        val lines = lineClusters(region)
        class Found(val key: GroupKey, val top: Float, val bottom: Float)
        val found = ArrayList<Found>()
        var year: Int? = null
        var semester: String? = null
        var top = 0f
        for (line in lines) {
            val text = line.joinToString(" ") { it.text }.trim()
            val lineTop = line.minOf { it.top }
            val lineBottom = line.maxOf { it.bottom }
            val yearMatch = YEAR_LINE.matchEntire(text)
            if (yearMatch != null) {
                year = yearMatch.groupValues[1].toInt()
                semester = null
                top = lineTop
                continue
            }
            val semMatch = SEM_LINE.matchEntire(text)
            if (semMatch != null && year != null) {
                semester = semMatch.groupValues[1].lowercase().replaceFirstChar { it.uppercase() }
                continue
            }
            val secMatch = SEC_LINE.matchEntire(text)
            if (secMatch != null && year != null && semester != null) {
                found += Found(GroupKey(year, semester, secMatch.groupValues[1].uppercase()), top, lineBottom)
                year = null
                semester = null
            }
        }
        if (found.isEmpty()) return emptyList()
        val gaps = found.zipWithNext { a, b -> b.top - a.bottom }
        val gap = if (gaps.isEmpty()) rowHeight(words) * 0.5f else gaps.sorted()[gaps.size / 2]
        return found.mapIndexed { i, item ->
            val bandTop = if (i == 0) block.headerBottom else (found[i - 1].bottom + item.top) / 2f
            val bandBottom = if (i == found.lastIndex) item.bottom + gap / 2f else (item.bottom + found[i + 1].top) / 2f
            RowBand(item.key, bandTop, bandBottom)
        }
    }

    // ---- Cells ---------------------------------------------------------------------------

    private fun hasAnyBoundary(raster: Raster, block: Block, bands: List<RowBand>): Boolean {
        for (band in bands) {
            for (k in 0 until block.centers.size - 1) {
                if (boundaryDrawn(raster, block, k, band)) return true
            }
        }
        return false
    }

    private fun boundaryDrawn(raster: Raster, block: Block, boundary: Int, band: RowBand): Boolean {
        val pad = ((band.bottom - band.top) * 0.08f).toInt() + 1
        val y0 = (band.top.toInt() + pad).coerceIn(0, raster.height - 1)
        val y1 = (band.bottom.toInt() - pad).coerceIn(0, raster.height - 1)
        if (y1 - y0 < 8) return false
        val center = block.edge(boundary)
        val window = (block.colWidth * 0.22f).toInt().coerceAtLeast(2)
        for (x in (center.toInt() - window)..(center.toInt() + window)) {
            if (x < 1 || x >= raster.width - 1) continue
            if (longestRun(raster, x, y0, y1) >= (y1 - y0) * BOUNDARY_RUN) return true
        }
        return false
    }

    /** Longest vertical stretch of ink at column [x] (one pixel either side counts), bridging 2 px gaps. */
    private fun longestRun(raster: Raster, x: Int, y0: Int, y1: Int): Int {
        var best = 0
        var start = -1
        var lastInk = -1
        for (y in y0..y1) {
            val ink = raster.luma(x - 1, y) < raster.threshold ||
                raster.luma(x, y) < raster.threshold || raster.luma(x + 1, y) < raster.threshold
            if (ink) {
                if (start < 0) start = y
                lastInk = y
            } else if (start >= 0 && y - lastInk > 2) {
                best = maxOf(best, lastInk - start + 1)
                start = -1
            }
        }
        if (start >= 0) best = maxOf(best, lastInk - start + 1)
        return best
    }

    private fun cellsOf(
        page: OfflinePage,
        block: Block,
        band: RowBand,
        times: List<Pair<String, String>>,
        lineMode: Boolean,
        warnings: MutableList<String>,
    ): List<OfflineCell> {
        val inBand = page.words.filter {
            it.centerX > block.left && it.centerX < block.right && it.centerY > band.top && it.centerY < band.bottom
        }
        if (inBand.isEmpty()) return emptyList()
        val n = block.centers.size

        val spans: List<IntRange> = if (lineMode && page.raster != null) {
            val ranges = ArrayList<IntRange>()
            var first = 0
            for (k in 0 until n - 1) {
                if (boundaryDrawn(page.raster, block, k, band)) {
                    ranges += first..k
                    first = k + 1
                }
            }
            ranges += first..(n - 1)
            ranges
        } else {
            fallbackSpans(inBand, block)
        }

        val cells = ArrayList<OfflineCell>()
        for (span in spans) {
            val xLo = if (span.first == 0) block.left else block.edge(span.first - 1)
            val xHi = if (span.last == n - 1) block.right else block.edge(span.last)
            val words = inBand.filter { it.centerX >= xLo && it.centerX < xHi }
            if (words.isEmpty()) continue
            val lines = lineClusters(words).map { line -> line.joinToString(" ") { it.text }.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue
            val last = minOf(span.last, times.size - 1)
            val first = minOf(span.first, last)
            cells += OfflineCell(
                group = band.group,
                day = block.day,
                first = first + 1,
                last = last + 1,
                startTime = times[first].first,
                endTime = times[last].second,
                lines = lines,
            )
        }
        return cells
    }

    /** Without drawn lines: group words by gaps, then widen lab blocks to the usual three periods. */
    private fun fallbackSpans(words: List<OcrWord>, block: Block): List<IntRange> {
        val n = block.centers.size
        val gap = rowHeight(words) * 0.55f
        val sorted = words.sortedBy { it.left }
        val clusters = ArrayList<MutableList<OcrWord>>()
        var reach = Float.NEGATIVE_INFINITY
        for (word in sorted) {
            if (clusters.isEmpty() || word.left - reach > gap) clusters += mutableListOf(word) else clusters.last() += word
            reach = maxOf(reach, word.right)
        }
        val spans = ArrayList<IntRange>()
        for (cluster in clusters) {
            val lo = cluster.minOf { it.left }
            val hi = cluster.maxOf { it.right }
            var first = (0 until n).firstOrNull { block.centers[it] + block.colWidth * 0.2f > lo } ?: continue
            var last = (n - 1 downTo 0).firstOrNull { block.centers[it] - block.colWidth * 0.2f < hi } ?: continue
            if (last < first) last = first
            val text = cluster.joinToString(" ") { it.text }
            if (last - first + 1 < 3 && ROOM_WORDS.containsMatchIn(text) && text.contains("lab", true)) {
                val middle = (first + last) / 2
                first = (middle - 1).coerceAtLeast(0)
                last = (first + 2).coerceAtMost(n - 1)
                first = (last - 2).coerceAtLeast(0)
            }
            spans += first..last
        }
        return spans.fold(ArrayList()) { acc, span ->
            val previous = acc.lastOrNull()
            if (previous != null && span.first <= previous.last) acc[acc.lastIndex] = previous.first..maxOf(previous.last, span.last)
            else acc += span
            acc
        }
    }

    // ---- Cell text -------------------------------------------------------------------------

    private fun splitTopLevel(text: String): List<String> {
        val parts = ArrayList<String>()
        val current = StringBuilder()
        var depth = 0
        for (ch in text) {
            when {
                ch == '(' -> { depth++; current.append(ch) }
                ch == ')' -> { depth = maxOf(0, depth - 1); current.append(ch) }
                ch == '/' && depth == 0 -> { parts += current.toString().trim(); current.clear() }
                else -> current.append(ch)
            }
        }
        parts += current.toString().trim()
        return parts.filter { it.isNotEmpty() }
    }

    private fun tidyTeacher(text: String): String =
        text.trim().removePrefix("(").removeSuffix(")")
            .replace(Regex("\\s*/\\s*"), " / ").replace(Regex("\\s+"), " ").trim()

    private fun isRoom(line: String): Boolean {
        val plain = line.trim()
        return ROOM_NUMBER.matches(plain) || ROOM_WORDS.containsMatchIn(plain)
    }

    private fun sessionsOfCell(cell: OfflineCell): List<ClassSession> {
        val codeLine = cell.lines.first()
        val codeParts = splitTopLevel(codeLine)
        if (codeParts.isEmpty() || !codeParts.all { CODE.matches(it.trim()) }) return emptyList()
        val rest = cell.lines.drop(1)
        val roomLine = rest.lastOrNull { isRoom(it) }.orEmpty()
        val teacherLine = rest.firstOrNull { !isRoom(it) }.orEmpty()

        val teachers: List<String>
        val rooms: List<String>
        if (codeParts.size > 1) {
            val teacherParts = splitTopLevel(teacherLine)
            val roomParts = splitTopLevel(roomLine)
            teachers = if (teacherParts.size == codeParts.size) teacherParts.map { tidyTeacher(it) }
            else codeParts.map { tidyTeacher(teacherLine) }
            rooms = if (roomParts.size == codeParts.size) roomParts else codeParts.map { roomLine }
        } else {
            teachers = listOf(tidyTeacher(teacherLine))
            rooms = listOf(roomLine.replace(Regex("\\s+"), " ").trim())
        }

        return codeParts.mapIndexed { i, code ->
            val room = rooms[i]
            ClassSession(
                dayOfWeek = cell.day,
                courseCode = CourseCodes.pretty(code.trim()),
                courseTitle = "",
                room = room,
                startTime = cell.startTime,
                endTime = cell.endTime,
                type = if (room.contains("lab", true)) SessionType.LAB else SessionType.LECTURE,
                teacher = teachers[i],
            )
        }
    }

    /** Joins back-to-back periods of the same class (same code, teacher and room) into one session. */
    private fun merge(sessions: List<ClassSession>): List<ClassSession> {
        val result = ArrayList<ClassSession>()
        for (session in sessions.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))) {
            val index = result.indexOfLast {
                it.dayOfWeek == session.dayOfWeek && it.courseCode == session.courseCode &&
                    it.teacher == session.teacher && it.room == session.room && it.endTime == session.startTime
            }
            if (index >= 0) result[index] = result[index].copy(endTime = session.endTime) else result += session
        }
        return result
    }
}

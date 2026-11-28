package com.example.nextclass.domain.offline

import com.example.nextclass.domain.model.ClassSession

/** One recognised word with its box in page pixels. */
data class OcrWord(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val height: Float get() = bottom - top
}

/** Grey-scale pixels of a page, used to see which grid lines are drawn. */
interface Raster {
    val width: Int
    val height: Int

    /** 0 is black, 255 is white. */
    fun luma(x: Int, y: Int): Int

    /** Pixels darker than this count as ink. */
    val threshold: Int
}

/** Words and (when available) the pixels of one page. Words and raster share one coordinate space. */
class OfflinePage(val words: List<OcrWord>, val raster: Raster?)

/** A row group of the timetable, such as 3rd Year, Odd Sem, Sec B. */
data class GroupKey(val year: Int, val semester: String, val section: String) {

    val label: String get() = "${ordinal(year)} Year, $semester Sem, Sec $section"

    /** True when every part of [filter] that names a year, semester or section agrees with this group. */
    fun matches(filter: String): Boolean {
        val text = filter.lowercase()
        if (text.isBlank()) return true
        Regex("\\b([1-9])\\s*(?:st|nd|rd|th)?\\s*year\\b").find(text)?.let {
            if (it.groupValues[1].toInt() != year) return false
        } ?: Regex("\\b([1-9])(?:st|nd|rd|th)\\b").find(text)?.let {
            if (it.groupValues[1].toInt() != year) return false
        }
        val wantsOdd = Regex("\\bodd\\b").containsMatchIn(text)
        val wantsEven = Regex("\\beven\\b").containsMatchIn(text)
        if (wantsOdd && !semester.equals("Odd", true)) return false
        if (wantsEven && !semester.equals("Even", true)) return false
        Regex("\\b(?:sec(?:tion)?|group)\\.?\\s*([a-z])\\b").find(text)?.let {
            if (!it.groupValues[1].equals(section, true)) return false
        }
        return true
    }

    private fun ordinal(n: Int): String = when (n) {
        1 -> "1st"
        2 -> "2nd"
        3 -> "3rd"
        else -> "${n}th"
    }
}

/** One filled block of the grid: periods [first]..[last] on [day], with its printed lines. */
class OfflineCell(
    val group: GroupKey,
    val day: Int,
    val first: Int,
    val last: Int,
    val startTime: String,
    val endTime: String,
    val lines: List<String>,
)

class TimetableGrid(
    val groups: List<GroupKey>,
    val cells: List<OfflineCell>,
    /** Things the reader could not make sense of, for the review screen. */
    val warnings: List<String>,
)

sealed interface OfflineOutcome {
    data class Found(val grid: TimetableGrid) : OfflineOutcome
    data class NotATimetable(val reason: String) : OfflineOutcome
}

sealed interface OfflineSelection {
    data class Ready(val group: GroupKey, val sessions: List<ClassSession>, val warnings: List<String>) : OfflineSelection
    data class NeedGroup(val groups: List<GroupKey>) : OfflineSelection
}

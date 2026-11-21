package com.example.nextclass.domain.model

import java.time.LocalTime
import java.util.Locale

enum class SessionType {
    LECTURE, LAB, TUTORIAL;

    companion object {
        fun fromRaw(raw: String?): SessionType =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: LECTURE
    }
}

data class ClassSession(
    val id: Long = 0L,
    val dayOfWeek: Int,
    val courseCode: String,
    val courseTitle: String,
    val room: String,
    val startTime: String,
    val endTime: String,
    val type: SessionType = SessionType.LECTURE,
) {
    val startLocalTime: LocalTime get() = LocalTime.parse(startTime)
    val endLocalTime: LocalTime get() = LocalTime.parse(endTime)
    val startMinutes: Int get() = TimeFormat.toMinutes(startTime)
    val endMinutes: Int get() = TimeFormat.toMinutes(endTime)
}

object DayNames {
    private val full = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    private val short = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    fun full(day: Int): String = full[(day - 1).coerceIn(0, 6)]

    fun short(day: Int): String = short[(day - 1).coerceIn(0, 6)]
}

object TimeFormat {

    private val TWELVE_HOUR = Regex("^(\\d{1,2})(?:[:.]?(\\d{2}))?\\s*([AaPp])\\.?\\s*[Mm]?\\.?$")

    /**
     * Accepts "9:00 AM", "9 pm", "9:00", "09:00", "9.00" and "0900" and returns zero-padded
     * 24-hour "HH:mm" (the stored format), or null when the text is not a time.
     */
    fun normalize(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        TWELVE_HOUR.matchEntire(text)?.let { match ->
            val hour12 = match.groupValues[1].toInt()
            val minute = match.groupValues[2].ifEmpty { "0" }.toInt()
            if (hour12 !in 1..12 || minute !in 0..59) return null
            val pm = match.groupValues[3].equals("p", ignoreCase = true)
            val hour24 = (hour12 % 12) + if (pm) 12 else 0
            return String.format(Locale.US, "%02d:%02d", hour24, minute)
        }
        val plain = text.replace('.', ':')
        val match = Regex("^(\\d{1,2}):?(\\d{2})$").matchEntire(plain) ?: return null
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        if (hour !in 0..23 || minute !in 0..59) return null
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    fun toMinutes(hhmm: String): Int {
        val parts = hhmm.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    fun range(session: ClassSession): String =
        "${clock12(session.startTime)} — ${clock12(session.endTime)}"

    fun duration(minutes: Int): String {
        val safe = minutes.coerceAtLeast(0)
        val hours = safe / 60
        val rest = safe % 60
        return if (hours > 0) String.format(Locale.US, "%dH %02dM", hours, rest)
        else String.format(Locale.US, "%dM", rest)
    }

    fun clock12(hhmm: String?): String {
        val normalized = normalize(hhmm) ?: return hhmm.orEmpty()
        val minutes = toMinutes(normalized)
        val hour24 = minutes / 60
        val minute = minutes % 60
        val suffix = if (hour24 < 12) "AM" else "PM"
        val hour12 = when (val h = hour24 % 12) {
            0 -> 12
            else -> h
        }
        return String.format(Locale.US, "%02d:%02d %s", hour12, minute, suffix)
    }
}

/** Course codes are compared without case, spaces or hyphens, so "CSE 2101" matches "cse-2101". */
object CourseCodes {
    fun key(code: String): String = code.filter { it.isLetterOrDigit() }.uppercase()

    /** Prints a code as LETTERS-DIGITS, for example CSE-2101. */
    fun pretty(code: String): String {
        val trimmed = code.trim().uppercase()
        val match = Regex("^([A-Z]+)[\\s-]*(\\d.*)$").matchEntire(trimmed) ?: return trimmed
        return match.groupValues[1] + "-" + match.groupValues[2].trim()
    }
}

data class Course(val code: String, val title: String) {
    val key: String get() = CourseCodes.key(code)
}

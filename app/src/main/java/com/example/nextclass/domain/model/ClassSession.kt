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

    /** Accepts "9:00", "09:00", "9.00" and "0900" and returns zero-padded "HH:mm", or null. */
    fun normalize(raw: String?): String? {
        val text = raw?.trim().orEmpty().replace('.', ':')
        val match = Regex("^(\\d{1,2}):?(\\d{2})$").matchEntire(text) ?: return null
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        if (hour !in 0..23 || minute !in 0..59) return null
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    fun toMinutes(hhmm: String): Int {
        val parts = hhmm.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    fun range(session: ClassSession): String = "${session.startTime} — ${session.endTime}"

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

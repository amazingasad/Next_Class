package com.example.nextclass.domain.model

import java.time.LocalDate

/** A change that applies to one class on one calendar date only. */
data class ClassOverride(
    val sessionId: Long,
    val date: String,
    val cancelled: Boolean,
    val room: String,
)

object RoutineOverrides {

    /** The coming calendar date (today included) that falls on [day], 1 = Monday. */
    fun nextDate(day: Int, today: LocalDate): LocalDate =
        today.plusDays(((day - today.dayOfWeek.value + 7) % 7).toLong())

    fun find(overrides: List<ClassOverride>, session: ClassSession, today: LocalDate): ClassOverride? {
        val date = nextDate(session.dayOfWeek, today).toString()
        return overrides.firstOrNull { it.sessionId == session.id && it.date == date }
    }

    /** Drops cancelled classes and applies room changes for the coming occurrence of each class. */
    fun effective(
        sessions: List<ClassSession>,
        overrides: List<ClassOverride>,
        today: LocalDate,
        breaks: List<BreakPeriod> = emptyList(),
    ): List<ClassSession> {
        if (overrides.isEmpty() && breaks.isEmpty()) return sessions
        return sessions.mapNotNull { session ->
            if (Breaks.on(nextDate(session.dayOfWeek, today), breaks) != null) return@mapNotNull null
            val override = find(overrides, session, today)
            when {
                override == null -> session
                override.cancelled -> null
                override.room.isNotBlank() -> session.copy(room = override.room)
                else -> session
            }
        }
    }
}

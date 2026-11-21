package com.example.nextclass.data.local

import com.example.nextclass.domain.model.ClassSession
import com.example.nextclass.domain.model.RoutineOverrides
import java.time.LocalDate

/** The saved routine with today's one-day changes applied: cancelled classes dropped, rooms swapped. */
suspend fun RoutineDao.effectiveSessions(today: LocalDate = LocalDate.now()): List<ClassSession> {
    val sessions = getAll().map { it.toDomain() }
    val overrides = getOverrides(today.minusDays(1).toString()).map { it.toDomain() }
    return RoutineOverrides.effective(sessions, overrides, today)
}

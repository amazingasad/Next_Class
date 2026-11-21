package com.example.nextclass.domain.usecase

import com.example.nextclass.domain.model.ClassSession
import com.example.nextclass.domain.model.DayNames
import com.example.nextclass.domain.model.NextClassState
import java.time.LocalDateTime

/**
 * Resolves what the user should see right now: the ongoing class, the next class today,
 * or a preview of the next scheduled day.
 */
class GetCurrentOrNextClassUseCase {

    operator fun invoke(
        sessions: List<ClassSession>,
        now: LocalDateTime = LocalDateTime.now(),
    ): NextClassState {
        val valid = sessions.filter { it.dayOfWeek in 1..7 }
        if (valid.isEmpty()) return NextClassState.NoClasses

        val today = now.dayOfWeek.value
        val nowMinutes = now.hour * 60 + now.minute
        val order = compareBy<ClassSession>({ it.startMinutes }, { it.endMinutes })
        val todays = valid.filter { it.dayOfWeek == today }.sortedWith(order)

        val ongoing = todays.firstOrNull { nowMinutes >= it.startMinutes && nowMinutes < it.endMinutes }
        if (ongoing != null) {
            val total = (ongoing.endMinutes - ongoing.startMinutes).coerceAtLeast(1)
            val elapsed = nowMinutes - ongoing.startMinutes
            return NextClassState.Ongoing(
                session = ongoing,
                minutesRemaining = ongoing.endMinutes - nowMinutes,
                progressFraction = (elapsed.toFloat() / total).coerceIn(0f, 1f),
            )
        }

        val upcoming = todays.firstOrNull { it.startMinutes > nowMinutes }
        if (upcoming != null) {
            return NextClassState.Upcoming(
                session = upcoming,
                minutesUntilStart = upcoming.startMinutes - nowMinutes,
            )
        }

        for (offset in 1..7) {
            val day = ((today - 1 + offset) % 7) + 1
            val first = valid.filter { it.dayOfWeek == day }.minWithOrNull(order) ?: continue
            val label = when (offset) {
                1 -> "Tomorrow"
                7 -> "Next ${DayNames.full(day)}"
                else -> DayNames.full(day)
            }
            return NextClassState.NextDayPreview(label, first)
        }
        return NextClassState.NoClasses
    }

    /**
     * The next moment at which [state] stops being true, so the widget can be refreshed with a
     * single exact alarm instead of polling. Always strictly later than [now].
     */
    fun nextBoundary(state: NextClassState, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? =
        when (state) {
            is NextClassState.Ongoing -> now.toLocalDate().atTime(state.session.endLocalTime)
            is NextClassState.Upcoming -> now.toLocalDate().atTime(state.session.startLocalTime)
            is NextClassState.NextDayPreview -> now.toLocalDate().plusDays(1).atStartOfDay()
            NextClassState.NoClasses -> null
        }
}

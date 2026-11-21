package com.example.nextclass.domain.model

sealed interface NextClassState {

    data class Ongoing(
        val session: ClassSession,
        val minutesRemaining: Int,
        val progressFraction: Float,
    ) : NextClassState

    data class Upcoming(
        val session: ClassSession,
        val minutesUntilStart: Int,
    ) : NextClassState

    data class NextDayPreview(
        val dayName: String,
        val firstSession: ClassSession,
    ) : NextClassState

    data object NoClasses : NextClassState
}

val NextClassState.focusSession: ClassSession?
    get() = when (this) {
        is NextClassState.Ongoing -> session
        is NextClassState.Upcoming -> session
        is NextClassState.NextDayPreview -> firstSession
        NextClassState.NoClasses -> null
    }

val NextClassState.statusLabel: String
    get() = when (this) {
        is NextClassState.Ongoing -> "NOW • ENDS ${session.endTime}"
        is NextClassState.Upcoming -> "NEXT • IN ${TimeFormat.duration(minutesUntilStart)}"
        is NextClassState.NextDayPreview ->
            "${dayName.uppercase()} • ${firstSession.startTime}"
        NextClassState.NoClasses -> "NO CLASSES"
    }

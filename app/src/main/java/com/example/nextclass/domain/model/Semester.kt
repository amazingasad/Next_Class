package com.example.nextclass.domain.model

/** A named term. Only the active semester drives the home screen, widgets and reminders. */
data class Semester(
    val id: Long,
    val name: String,
    val active: Boolean,
)

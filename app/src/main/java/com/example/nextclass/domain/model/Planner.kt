package com.example.nextclass.domain.model

/** One to-do item for a calendar date (yyyy-MM-dd). */
data class PlannerTask(
    val id: Long,
    val date: String,
    val title: String,
    val done: Boolean,
)

/** A study note. The pinned note is the one the planner widget shows. */
data class Note(
    val id: Long,
    val title: String,
    val body: String,
    val pinned: Boolean,
    val updatedAt: Long,
) {
    /** Non-blank lines of the body, in order. */
    val lines: List<String> get() = body.lines().map { it.trim() }.filter { it.isNotEmpty() }
}

package com.example.nextclass.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.nextclass.domain.model.BreakPeriod
import com.example.nextclass.domain.model.Note
import com.example.nextclass.domain.model.PlannerTask
import com.example.nextclass.domain.model.StudyEntry
import java.time.LocalDate

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val title: String,
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val pinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

fun TaskEntity.toDomain(): PlannerTask = PlannerTask(id = id, date = date, title = title, done = done)

fun NoteEntity.toDomain(): Note =
    Note(id = id, title = title, body = body, pinned = pinned, updatedAt = updatedAt)

@Entity(tableName = "breaks")
data class BreakEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startDate: String,
    val endDate: String,
)

@Entity(tableName = "study_log")
data class StudyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseKey: String,
    val courseCode: String,
    val date: String,
    val minutes: Int,
)

fun BreakEntity.toDomain(): BreakPeriod =
    BreakPeriod(id = id, name = name, start = LocalDate.parse(startDate), end = LocalDate.parse(endDate))

fun StudyLogEntity.toDomain(): StudyEntry =
    StudyEntry(id = id, courseKey = courseKey, courseCode = courseCode, date = date, minutes = minutes)

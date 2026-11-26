package com.example.nextclass.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.nextclass.domain.model.Note
import com.example.nextclass.domain.model.PlannerTask

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

package com.example.nextclass.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.nextclass.domain.model.ClassSession
import com.example.nextclass.domain.model.SessionType

@Entity(tableName = "class_sessions")
data class ClassSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int,
    val courseCode: String,
    val courseTitle: String,
    val room: String,
    val startTime: String,
    val endTime: String,
    val type: String

)

fun ClassSessionEntity.toDomain(): ClassSession = ClassSession(
    id = id,
    dayOfWeek = dayOfWeek,
    courseCode = courseCode,
    courseTitle = courseTitle,
    room = room,
    startTime = startTime,
    endTime = endTime,
    type = SessionType.fromRaw(type),
)

fun ClassSession.toEntity(): ClassSessionEntity = ClassSessionEntity(
    id = id,
    dayOfWeek = dayOfWeek,
    courseCode = courseCode,
    courseTitle = courseTitle,
    room = room,
    startTime = startTime,
    endTime = endTime,
    type = type.name
)

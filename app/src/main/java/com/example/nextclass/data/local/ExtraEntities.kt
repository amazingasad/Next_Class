package com.example.nextclass.data.local

import androidx.room.Entity
import com.example.nextclass.domain.model.ClassOverride
import com.example.nextclass.domain.model.Course
import com.example.nextclass.domain.model.CourseCodes

@Entity(tableName = "class_overrides", primaryKeys = ["sessionId", "date"])
data class ClassOverrideEntity(
    val sessionId: Long,
    val date: String,
    val cancelled: Boolean,
    val room: String,
)

fun ClassOverrideEntity.toDomain(): ClassOverride =
    ClassOverride(sessionId = sessionId, date = date, cancelled = cancelled, room = room)

fun ClassOverride.toEntity(): ClassOverrideEntity =
    ClassOverrideEntity(sessionId = sessionId, date = date, cancelled = cancelled, room = room)

@Entity(tableName = "courses", primaryKeys = ["codeKey"])
data class CourseEntity(
    val codeKey: String,
    val code: String,
    val title: String,
)

fun CourseEntity.toDomain(): Course = Course(code = code, title = title)

fun Course.toEntity(): CourseEntity =
    CourseEntity(codeKey = CourseCodes.key(code), code = code, title = title)

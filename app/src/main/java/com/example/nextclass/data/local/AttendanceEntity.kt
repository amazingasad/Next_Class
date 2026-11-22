package com.example.nextclass.data.local

import androidx.room.Entity
import com.example.nextclass.domain.model.AttendanceMark

@Entity(tableName = "attendance", primaryKeys = ["courseKey", "date", "startTime"])
data class AttendanceEntity(
    val courseKey: String,
    val date: String,
    val startTime: String,
    val status: String,
)

fun AttendanceEntity.toDomain(): AttendanceMark =
    AttendanceMark(courseKey = courseKey, date = date, startTime = startTime, present = status == "P")

fun AttendanceMark.toEntity(): AttendanceEntity =
    AttendanceEntity(courseKey = courseKey, date = date, startTime = startTime, status = if (present) "P" else "A")

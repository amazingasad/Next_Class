package com.example.nextclass.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.nextclass.domain.model.Semester

@Entity(tableName = "semesters")
data class SemesterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val active: Boolean = false,
)

fun SemesterEntity.toDomain(): Semester = Semester(id = id, name = name, active = active)

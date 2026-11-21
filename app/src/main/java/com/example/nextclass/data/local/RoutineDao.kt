package com.example.nextclass.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Query("SELECT * FROM class_sessions ORDER BY dayOfWeek ASC, startTime ASC")
    fun observeAll(): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getAll(): List<ClassSessionEntity>

    @Query("SELECT * FROM class_sessions WHERE dayOfWeek = :day ORDER BY startTime ASC")
    suspend fun getForDay(day: Int): List<ClassSessionEntity>

    @Query("SELECT * FROM class_sessions WHERE dayOfWeek = :day ORDER BY startTime ASC LIMIT 1")
    suspend fun getFirstClassOfDay(day: Int): ClassSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ClassSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<ClassSessionEntity>)

    @Query("DELETE FROM class_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM class_sessions")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(sessions: List<ClassSessionEntity>) {
        clearAll()
        clearOverrides()
        insertAll(sessions)
    }

    // ---- One-day changes ----

    @Query("SELECT * FROM class_overrides WHERE date >= :from")
    fun observeOverrides(from: String): Flow<List<ClassOverrideEntity>>

    @Query("SELECT * FROM class_overrides WHERE date >= :from")
    suspend fun getOverrides(from: String): List<ClassOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverride(override: ClassOverrideEntity)

    @Query("DELETE FROM class_overrides WHERE sessionId = :sessionId AND date = :date")
    suspend fun deleteOverride(sessionId: Long, date: String)

    @Query("DELETE FROM class_overrides WHERE date < :before")
    suspend fun deleteOverridesBefore(before: String)

    @Query("DELETE FROM class_overrides")
    suspend fun clearOverrides()

    // ---- Course list ----

    @Query("SELECT * FROM courses ORDER BY code ASC")
    fun observeCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses")
    suspend fun getCourses(): List<CourseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCourses(courses: List<CourseEntity>)

    @Query("DELETE FROM courses WHERE codeKey = :codeKey")
    suspend fun deleteCourse(codeKey: String)

    @Query("DELETE FROM courses")
    suspend fun clearCourses()
}

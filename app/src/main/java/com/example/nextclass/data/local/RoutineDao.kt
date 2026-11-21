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
        insertAll(sessions)
    }
}

package com.example.nextclass.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {

    // ---- Tasks ----

    @Query("SELECT * FROM tasks WHERE date >= :from ORDER BY date ASC, done ASC, id ASC")
    fun observeTasks(from: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY done ASC, id ASC")
    suspend fun tasksOn(date: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE date >= :from AND date <= :to")
    suspend fun tasksBetween(from: String, to: String): List<TaskEntity>

    @Insert
    suspend fun insertTask(task: TaskEntity): Long

    @Insert
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("SELECT done FROM tasks WHERE id = :id")
    suspend fun isDone(id: Long): Boolean?

    @Query("UPDATE tasks SET title = :title WHERE id = :id")
    suspend fun renameTask(id: Long, title: String)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("DELETE FROM tasks WHERE date < :before")
    suspend fun deleteTasksBefore(before: String)

    @Transaction
    suspend fun toggle(id: Long) {
        val current = isDone(id) ?: return
        setDone(id, !current)
    }

    // ---- Notes ----

    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    suspend fun getNotes(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("DELETE FROM notes")
    suspend fun clearNotes()

    @Query("UPDATE notes SET pinned = 0")
    suspend fun clearPins()

    /** Saves a note; a pinned note unpins every other one so the widget has a single note. */
    @Transaction
    suspend fun saveNote(note: NoteEntity): Long {
        if (note.pinned) clearPins()
        return upsertNote(note)
    }

    @Transaction
    suspend fun replaceNotes(notes: List<NoteEntity>) {
        clearNotes()
        insertNotes(notes)
    }

    // ---- Study log ----

    @Insert
    suspend fun insertStudy(entry: StudyLogEntity): Long

    @Query("SELECT * FROM study_log WHERE date >= :from ORDER BY id DESC")
    fun observeStudySince(from: String): Flow<List<StudyLogEntity>>

    @Query("SELECT * FROM study_log WHERE date >= :from AND date <= :to")
    suspend fun studyBetween(from: String, to: String): List<StudyLogEntity>
}

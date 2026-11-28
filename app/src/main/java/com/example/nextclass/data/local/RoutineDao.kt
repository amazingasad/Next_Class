package com.example.nextclass.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Every routine query is scoped to the active semester. Inserts stamp the active semester id, so
 * callers never deal with semesters except through the semester functions at the bottom.
 */
@Dao
interface RoutineDao {

    @Query("SELECT id FROM semesters WHERE active = 1 LIMIT 1")
    suspend fun activeSemesterIdOrNull(): Long?

    suspend fun activeSemesterId(): Long = activeSemesterIdOrNull() ?: 1L

    // ---- Classes ----

    @Query(
        "SELECT * FROM class_sessions WHERE semesterId = (SELECT id FROM semesters WHERE active = 1 LIMIT 1) " +
            "ORDER BY dayOfWeek ASC, startTime ASC",
    )
    fun observeAll(): Flow<List<ClassSessionEntity>>

    @Query(
        "SELECT * FROM class_sessions WHERE semesterId = (SELECT id FROM semesters WHERE active = 1 LIMIT 1) " +
            "ORDER BY dayOfWeek ASC, startTime ASC",
    )
    suspend fun getAll(): List<ClassSessionEntity>

    @Query(
        "SELECT * FROM class_sessions WHERE dayOfWeek = :day AND " +
            "semesterId = (SELECT id FROM semesters WHERE active = 1 LIMIT 1) ORDER BY startTime ASC",
    )
    suspend fun getForDay(day: Int): List<ClassSessionEntity>

    @Query(
        "SELECT * FROM class_sessions WHERE dayOfWeek = :day AND " +
            "semesterId = (SELECT id FROM semesters WHERE active = 1 LIMIT 1) ORDER BY startTime ASC LIMIT 1",
    )
    suspend fun getFirstClassOfDay(day: Int): ClassSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRaw(session: ClassSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRaw(sessions: List<ClassSessionEntity>)

    @Transaction
    suspend fun upsert(session: ClassSessionEntity): Long =
        upsertRaw(session.copy(semesterId = activeSemesterId()))

    @Transaction
    suspend fun insertAll(sessions: List<ClassSessionEntity>) {
        val semester = activeSemesterId()
        insertAllRaw(sessions.map { it.copy(semesterId = semester) })
    }

    @Query("DELETE FROM class_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM class_sessions WHERE semesterId = (SELECT id FROM semesters WHERE active = 1 LIMIT 1)")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(sessions: List<ClassSessionEntity>) {
        clearOverrides()
        clearAll()
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

    @Query(
        "DELETE FROM class_overrides WHERE sessionId IN (SELECT id FROM class_sessions WHERE " +
            "semesterId = (SELECT id FROM semesters WHERE active = 1 LIMIT 1))",
    )
    suspend fun clearOverrides()

    // ---- Course list ----

    @Query(
        "SELECT * FROM courses WHERE semesterId = (SELECT id FROM semesters WHERE active = 1 LIMIT 1) " +
            "ORDER BY code ASC",
    )
    fun observeCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE semesterId = (SELECT id FROM semesters WHERE active = 1 LIMIT 1)")
    suspend fun getCourses(): List<CourseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCoursesRaw(courses: List<CourseEntity>)

    @Transaction
    suspend fun upsertCourses(courses: List<CourseEntity>) {
        val semester = activeSemesterId()
        upsertCoursesRaw(courses.map { it.copy(semesterId = semester) })
    }

    @Query(
        "DELETE FROM courses WHERE codeKey = :codeKey AND " +
            "semesterId = (SELECT id FROM semesters WHERE active = 1 LIMIT 1)",
    )
    suspend fun deleteCourse(codeKey: String)

    @Query("DELETE FROM courses WHERE semesterId = (SELECT id FROM semesters WHERE active = 1 LIMIT 1)")
    suspend fun clearCourses()

    // ---- Attendance ----

    @Query("SELECT * FROM attendance WHERE semesterId = (SELECT id FROM semesters WHERE active = 1 LIMIT 1)")
    fun observeAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE semesterId = (SELECT id FROM semesters WHERE active = 1 LIMIT 1)")
    suspend fun getAttendance(): List<AttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendanceRaw(mark: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRaw(marks: List<AttendanceEntity>)

    @Transaction
    suspend fun upsertAttendance(mark: AttendanceEntity) {
        upsertAttendanceRaw(mark.copy(semesterId = activeSemesterId()))
    }

    @Transaction
    suspend fun insertAttendance(marks: List<AttendanceEntity>) {
        val semester = activeSemesterId()
        insertAttendanceRaw(marks.map { it.copy(semesterId = semester) })
    }

    @Query(
        "DELETE FROM attendance WHERE courseKey = :courseKey AND date = :date AND startTime = :startTime AND " +
            "semesterId = (SELECT id FROM semesters WHERE active = 1 LIMIT 1)",
    )
    suspend fun deleteAttendance(courseKey: String, date: String, startTime: String)

    @Query("DELETE FROM attendance WHERE semesterId = (SELECT id FROM semesters WHERE active = 1 LIMIT 1)")
    suspend fun clearAttendance()

    @Transaction
    suspend fun replaceAttendance(marks: List<AttendanceEntity>) {
        clearAttendance()
        insertAttendance(marks)
    }

    // ---- Semesters ----

    @Query("SELECT * FROM semesters ORDER BY id ASC")
    fun observeSemesters(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters ORDER BY id ASC")
    suspend fun getSemesters(): List<SemesterEntity>

    @Insert
    suspend fun insertSemester(semester: SemesterEntity): Long

    @Query("UPDATE semesters SET active = 0")
    suspend fun deactivateSemesters()

    @Query("UPDATE semesters SET active = 1 WHERE id = :id")
    suspend fun activateSemester(id: Long)

    @Query("UPDATE semesters SET name = :name WHERE id = :id")
    suspend fun renameSemester(id: Long, name: String)

    @Query("DELETE FROM semesters WHERE id = :id")
    suspend fun deleteSemesterRow(id: Long)

    @Query("DELETE FROM class_overrides WHERE sessionId IN (SELECT id FROM class_sessions WHERE semesterId = :id)")
    suspend fun deleteOverridesOfSemester(id: Long)

    @Query("DELETE FROM class_sessions WHERE semesterId = :id")
    suspend fun deleteSessionsOfSemester(id: Long)

    @Query("DELETE FROM courses WHERE semesterId = :id")
    suspend fun deleteCoursesOfSemester(id: Long)

    @Query("DELETE FROM attendance WHERE semesterId = :id")
    suspend fun deleteAttendanceOfSemester(id: Long)

    @Transaction
    suspend fun switchSemester(id: Long) {
        deactivateSemesters()
        activateSemester(id)
    }

    @Transaction
    suspend fun createSemester(name: String): Long {
        deactivateSemesters()
        return insertSemester(SemesterEntity(name = name, active = true))
    }

    /** Removes a semester with everything in it. The last remaining semester cannot be removed. */
    @Transaction
    suspend fun removeSemester(id: Long): Boolean {
        val all = getSemesters()
        if (all.size <= 1 || all.none { it.id == id }) return false
        val wasActive = all.first { it.id == id }.active
        deleteOverridesOfSemester(id)
        deleteSessionsOfSemester(id)
        deleteCoursesOfSemester(id)
        deleteAttendanceOfSemester(id)
        deleteSemesterRow(id)
        if (wasActive) {
            val next = all.first { it.id != id }
            activateSemester(next.id)
        }
        return true
    }

    // ---- Breaks ----

    @Query("SELECT * FROM breaks ORDER BY startDate ASC")
    fun observeBreaks(): Flow<List<BreakEntity>>

    @Query("SELECT * FROM breaks ORDER BY startDate ASC")
    suspend fun getBreaks(): List<BreakEntity>

    @Insert
    suspend fun insertBreak(item: BreakEntity): Long

    @Query("DELETE FROM breaks WHERE id = :id")
    suspend fun deleteBreak(id: Long)

    @Query("DELETE FROM breaks WHERE endDate < :before")
    suspend fun deleteBreaksBefore(before: String)
}

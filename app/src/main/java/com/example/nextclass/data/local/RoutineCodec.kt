package com.example.nextclass.data.local

import android.util.Base64
import com.example.nextclass.domain.model.AttendanceMark
import com.example.nextclass.domain.model.ClassSession
import com.example.nextclass.domain.model.Note
import com.example.nextclass.domain.model.Course
import com.example.nextclass.domain.model.CourseCodes
import com.example.nextclass.domain.model.SessionType
import com.example.nextclass.domain.model.TimeFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@Serializable
data class BundleSession(
    val dayOfWeek: Int,
    val courseCode: String,
    val courseTitle: String = "",
    val room: String = "",
    val startTime: String,
    val endTime: String,
    val type: String = "LECTURE",
    val teacher: String = "",
)

@Serializable
data class BundleCourse(
    val code: String,
    val title: String = "",
)

@Serializable
data class BundleAttendance(
    val course: String,
    val date: String,
    val start: String,
    val present: Boolean,
)

@Serializable
data class BundleNote(
    val title: String = "",
    val body: String = "",
    val pinned: Boolean = false,
)

@Serializable
data class RoutineBundle(
    val app: String = "nextclass",
    val version: Int = 1,
    val sessions: List<BundleSession> = emptyList(),
    val courses: List<BundleCourse> = emptyList(),
    val attendance: List<BundleAttendance> = emptyList(),
    val notes: List<BundleNote> = emptyList(),
)

/** Turns the routine into a backup file or a short share code, and back. */
object RoutineCodec {

    private const val SHARE_PREFIX = "NXC1:"
    private val SHARE_PATTERN = Regex("NXC1:([A-Za-z0-9_-]+)")

    private val compact = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val pretty = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    fun bundleOf(
        sessions: List<ClassSession>,
        courses: List<Course>,
        attendance: List<AttendanceMark> = emptyList(),
        notes: List<Note> = emptyList(),
    ): RoutineBundle = RoutineBundle(
        sessions = sessions.map {
            BundleSession(
                dayOfWeek = it.dayOfWeek,
                courseCode = it.courseCode,
                courseTitle = it.courseTitle,
                room = it.room,
                startTime = it.startTime,
                endTime = it.endTime,
                type = it.type.name,
                teacher = it.teacher,
            )
        },
        courses = courses.map { BundleCourse(code = it.code, title = it.title) },
        notes = notes.map { BundleNote(title = it.title, body = it.body, pinned = it.pinned) },
        attendance = attendance.map {
            BundleAttendance(course = it.courseKey, date = it.date, start = it.startTime, present = it.present)
        },
    )

    fun toBackupJson(bundle: RoutineBundle): String = pretty.encodeToString(RoutineBundle.serializer(), bundle)

    fun toShareCode(bundle: RoutineBundle): String {
        val raw = compact.encodeToString(RoutineBundle.serializer(), bundle).toByteArray(Charsets.UTF_8)
        val zipped = ByteArrayOutputStream().also { out ->
            GZIPOutputStream(out).use { it.write(raw) }
        }.toByteArray()
        return SHARE_PREFIX + Base64.encodeToString(zipped, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun shareMessage(bundle: RoutineBundle): String =
        "NextClass routine (${bundle.sessions.size} classes)\n" +
            "Open NextClass, go to Settings, choose Import shared routine and paste this whole message.\n\n" +
            toShareCode(bundle)

    /** Reads either a backup file's text or a pasted share message. Returns null if it is neither. */
    fun parse(text: String): RoutineBundle? {
        val trimmed = text.trim()
        val bundle = try {
            if (trimmed.startsWith("{")) {
                compact.decodeFromString(RoutineBundle.serializer(), trimmed)
            } else {
                val code = SHARE_PATTERN.find(trimmed)?.groupValues?.get(1) ?: return null
                val zipped = Base64.decode(code, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                val json = GZIPInputStream(ByteArrayInputStream(zipped)).use { it.readBytes() }
                    .toString(Charsets.UTF_8)
                compact.decodeFromString(RoutineBundle.serializer(), json)
            }
        } catch (e: Exception) {
            return null
        }
        return sanitize(bundle)
    }

    /** Drops rows with impossible days or times so a damaged file cannot crash the widget. */
    private fun sanitize(bundle: RoutineBundle): RoutineBundle {
        val sessions = bundle.sessions.mapNotNull { row ->
            val start = TimeFormat.normalize(row.startTime) ?: return@mapNotNull null
            val end = TimeFormat.normalize(row.endTime) ?: return@mapNotNull null
            if (row.dayOfWeek !in 1..7 || row.courseCode.isBlank()) return@mapNotNull null
            if (TimeFormat.toMinutes(end) <= TimeFormat.toMinutes(start)) return@mapNotNull null
            row.copy(
                courseCode = row.courseCode.trim().uppercase(),
                startTime = start,
                endTime = end,
                type = SessionType.fromRaw(row.type).name,
            )
        }
        val courses = bundle.courses
            .filter { it.code.isNotBlank() && CourseCodes.key(it.code).isNotEmpty() }
            .map { it.copy(code = CourseCodes.pretty(it.code), title = it.title.trim()) }
        val attendance = bundle.attendance.mapNotNull { row ->
            val key = CourseCodes.key(row.course)
            val date = try {
                java.time.LocalDate.parse(row.date).toString()
            } catch (e: Exception) {
                return@mapNotNull null
            }
            val start = TimeFormat.normalize(row.start) ?: return@mapNotNull null
            if (key.isEmpty()) null else BundleAttendance(key, date, start, row.present)
        }
        return bundle.copy(sessions = sessions, courses = courses, attendance = attendance)
    }

    fun toSessions(bundle: RoutineBundle): List<ClassSession> = bundle.sessions.map {
        ClassSession(
            dayOfWeek = it.dayOfWeek,
            courseCode = it.courseCode,
            courseTitle = it.courseTitle,
            room = it.room,
            startTime = it.startTime,
            endTime = it.endTime,
            type = SessionType.fromRaw(it.type),
            teacher = it.teacher,
        )
    }

    fun toCourses(bundle: RoutineBundle): List<Course> =
        bundle.courses.map { Course(code = it.code, title = it.title) }

    fun toNotes(bundle: RoutineBundle): List<BundleNote> =
        bundle.notes.filter { it.title.isNotBlank() || it.body.isNotBlank() }

    fun toAttendance(bundle: RoutineBundle): List<AttendanceMark> = bundle.attendance.map {
        AttendanceMark(courseKey = it.course, date = it.date, startTime = it.start, present = it.present)
    }
}

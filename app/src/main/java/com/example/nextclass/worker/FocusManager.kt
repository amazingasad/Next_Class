package com.example.nextclass.worker

import android.content.Context
import android.content.Intent
import com.example.nextclass.data.local.AppDatabase
import com.example.nextclass.data.local.AppPreferences
import com.example.nextclass.data.local.StudyLogEntity
import com.example.nextclass.domain.model.FocusSession
import java.time.LocalDate

/**
 * The study timer. Its state lives in preferences with a wall-clock end time, so it keeps counting
 * while the app is closed, and an alarm tells the user when it is done.
 */
object FocusManager {

    private val lock = Any()

    fun load(context: Context): FocusSession? {
        val prefs = AppPreferences(context)
        if (prefs.focusTotalSec <= 0 || prefs.focusCourseKey.isEmpty()) return null
        return FocusSession(
            courseKey = prefs.focusCourseKey,
            courseCode = prefs.focusCourseCode,
            totalSec = prefs.focusTotalSec,
            endAt = prefs.focusEndAt,
            pausedLeftSec = prefs.focusPausedLeft,
        )
    }

    fun start(context: Context, courseKey: String, courseCode: String, minutes: Int) {
        val appContext = context.applicationContext
        synchronized(lock) {
            val total = minutes * 60
            val endAt = System.currentTimeMillis() + total * 1000L
            AppPreferences(appContext).writeFocus(courseKey, courseCode, total, endAt, -1)
            arm(appContext, endAt)
        }
    }

    fun pause(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            val session = load(appContext) ?: return
            if (!session.running) return
            val left = session.leftSec(System.currentTimeMillis())
            AppPreferences(appContext)
                .writeFocus(session.courseKey, session.courseCode, session.totalSec, 0L, left)
            disarm(appContext)
        }
    }

    fun resume(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            val session = load(appContext) ?: return
            if (session.running) return
            val endAt = System.currentTimeMillis() + session.pausedLeftSec * 1000L
            AppPreferences(appContext)
                .writeFocus(session.courseKey, session.courseCode, session.totalSec, endAt, -1)
            arm(appContext, endAt)
        }
    }

    /** Throws the session away without logging anything. */
    fun discard(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            AppPreferences(appContext).writeFocus("", "", 0, 0L, -1)
            disarm(appContext)
        }
    }

    /**
     * Ends the session and logs the time actually studied. Returns the minutes logged, or 0 when
     * there was no session or under half a minute passed. Safe to call twice.
     */
    suspend fun finish(context: Context): Int {
        val appContext = context.applicationContext
        val session: FocusSession
        val studiedSec: Int
        synchronized(lock) {
            session = load(appContext) ?: return 0
            studiedSec = session.totalSec - session.leftSec(System.currentTimeMillis())
            AppPreferences(appContext).writeFocus("", "", 0, 0L, -1)
            disarm(appContext)
        }
        val minutes = (studiedSec + 30) / 60
        if (minutes <= 0) return 0
        AppDatabase.getInstance(appContext).plannerDao().insertStudy(
            StudyLogEntity(
                courseKey = session.courseKey,
                courseCode = session.courseCode,
                date = LocalDate.now().toString(),
                minutes = minutes,
            ),
        )
        return minutes
    }

    private fun arm(context: Context, endAt: Long) {
        val pending = AlarmHelper.pending(
            context,
            AlarmHelper.REQ_FOCUS,
            Intent(context, FocusDoneReceiver::class.java),
        )
        AlarmHelper.schedule(context, endAt, pending)
    }

    private fun disarm(context: Context) {
        val pending = AlarmHelper.pending(
            context,
            AlarmHelper.REQ_FOCUS,
            Intent(context, FocusDoneReceiver::class.java),
        )
        AlarmHelper.cancel(context, pending)
    }
}

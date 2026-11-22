package com.example.nextclass.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.nextclass.data.local.AppDatabase
import com.example.nextclass.data.local.AttendanceEntity
import com.example.nextclass.domain.model.CourseCodes
import java.time.LocalDate

/** Widget button: marks today's class present or absent. Tapping the chosen mark again clears it. */
class MarkAttendanceAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val sessionId = parameters[SESSION_ID] ?: return
        val present = parameters[PRESENT] ?: return
        val dao = AppDatabase.getInstance(context).routineDao()
        val session = dao.getAll().firstOrNull { it.id == sessionId } ?: return
        val key = CourseCodes.key(session.courseCode)
        val date = LocalDate.now().toString()
        val wanted = if (present) "P" else "A"
        val existing = dao.getAttendance().firstOrNull {
            it.courseKey == key && it.date == date && it.startTime == session.startTime
        }
        if (existing?.status == wanted) {
            dao.deleteAttendance(key, date, session.startTime)
        } else {
            dao.upsertAttendance(AttendanceEntity(key, date, session.startTime, wanted))
        }
        WidgetUpdateHelper.updateWidgets(context)
    }

    companion object {
        val SESSION_ID = ActionParameters.Key<Long>("attendanceSessionId")
        val PRESENT = ActionParameters.Key<Boolean>("attendancePresent")
    }
}

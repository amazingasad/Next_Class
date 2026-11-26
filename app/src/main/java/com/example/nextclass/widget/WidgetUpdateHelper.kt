package com.example.nextclass.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.example.nextclass.data.local.AppDatabase
import com.example.nextclass.data.local.AppPreferences
import com.example.nextclass.data.local.effectiveSessions
import com.example.nextclass.domain.usecase.GetCurrentOrNextClassUseCase
import com.example.nextclass.domain.usecase.ScheduleDailyFirstClassAlarmUseCase
import com.example.nextclass.worker.AlarmHelper
import com.example.nextclass.worker.PlanReminderScheduler
import com.example.nextclass.worker.WidgetTransitionReceiver
import java.time.LocalDateTime

object WidgetUpdateHelper {

    /** Redraw the widget, arm the next state-change alarm, and refresh the daily reminder. */
    suspend fun refreshAll(context: Context) {
        val appContext = context.applicationContext
        val dao = AppDatabase.getInstance(appContext).routineDao()
        dao.deleteOverridesBefore(java.time.LocalDate.now().toString())
        AppDatabase.getInstance(appContext).plannerDao()
            .deleteTasksBefore(java.time.LocalDate.now().minusDays(14).toString())
        updateWidgets(appContext)
        scheduleNextTransition(appContext)
        PlanReminderScheduler.schedule(appContext)
        ScheduleDailyFirstClassAlarmUseCase(appContext, dao, AppPreferences(appContext)).invoke()
    }

    suspend fun updateWidgets(context: Context) {
        NextClassWidget().updateAll(context.applicationContext)
        NextClassCompactWidget().updateAll(context.applicationContext)
        NextClassPlannerWidget().updateAll(context.applicationContext)
    }

    /**
     * Schedules one exact alarm for the next moment the widget content changes: the start of the
     * upcoming class or the end of the ongoing one. No periodic polling.
     */
    suspend fun scheduleNextTransition(context: Context, now: LocalDateTime = LocalDateTime.now()) {
        val appContext = context.applicationContext
        val sessions = AppDatabase.getInstance(appContext).routineDao().effectiveSessions()
        val useCase = GetCurrentOrNextClassUseCase()
        val state = useCase(sessions, now)
        val boundary = useCase.nextBoundary(state, now)

        val intent = Intent(appContext, WidgetTransitionReceiver::class.java)
        val pending = AlarmHelper.pending(appContext, AlarmHelper.REQ_TRANSITION, intent)
        if (boundary == null) {
            AlarmHelper.cancel(appContext, pending)
        } else {
            AlarmHelper.schedule(appContext, AlarmHelper.millis(boundary), pending)
        }
    }
}

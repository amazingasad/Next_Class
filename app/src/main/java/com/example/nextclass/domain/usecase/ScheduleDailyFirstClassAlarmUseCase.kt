package com.example.nextclass.domain.usecase

import android.content.Context
import android.content.Intent
import com.example.nextclass.data.local.AppPreferences
import com.example.nextclass.data.local.RoutineDao
import com.example.nextclass.data.local.toDomain
import com.example.nextclass.worker.AlarmHelper
import com.example.nextclass.worker.FirstClassNotificationReceiver
import com.example.nextclass.worker.WidgetTransitionReceiver
import java.time.LocalDateTime

/**
 * Schedules exactly one reminder per day, for the first class of today, and a 00:05 alarm that
 * rolls the schedule over to the next day. Second and later classes never get an alarm.
 */
class ScheduleDailyFirstClassAlarmUseCase(
    context: Context,
    private val dao: RoutineDao,
    private val preferences: AppPreferences,
) {
    private val appContext = context.applicationContext

    suspend operator fun invoke(now: LocalDateTime = LocalDateTime.now()) {
        scheduleMidnightRollover(now)

        val reminderIntent = Intent(appContext, FirstClassNotificationReceiver::class.java)
        val reminderPending = AlarmHelper.pending(appContext, AlarmHelper.REQ_FIRST_CLASS, reminderIntent)
        AlarmHelper.cancel(appContext, reminderPending)

        if (!preferences.firstClassReminderEnabled) return

        val first = dao.getFirstClassOfDay(now.dayOfWeek.value)?.toDomain() ?: return
        val lead = preferences.leadMinutes
        val trigger = now.toLocalDate().atTime(first.startLocalTime).minusMinutes(lead.toLong())
        if (!trigger.isAfter(now)) return

        val intent = Intent(appContext, FirstClassNotificationReceiver::class.java)
            .putExtra(FirstClassNotificationReceiver.EXTRA_CODE, first.courseCode)
            .putExtra(FirstClassNotificationReceiver.EXTRA_ROOM, first.room)
            .putExtra(FirstClassNotificationReceiver.EXTRA_START, first.startTime)
            .putExtra(FirstClassNotificationReceiver.EXTRA_LEAD, lead)
        val pending = AlarmHelper.pending(appContext, AlarmHelper.REQ_FIRST_CLASS, intent)
        AlarmHelper.schedule(appContext, AlarmHelper.millis(trigger), pending)
    }

    private fun scheduleMidnightRollover(now: LocalDateTime) {
        val next = now.toLocalDate().plusDays(1).atTime(0, 5)
        val intent = Intent(appContext, WidgetTransitionReceiver::class.java)
            .setAction(WidgetTransitionReceiver.ACTION_MIDNIGHT)
        val pending = AlarmHelper.pending(appContext, AlarmHelper.REQ_MIDNIGHT, intent)
        AlarmHelper.schedule(appContext, AlarmHelper.millis(next), pending)
    }
}

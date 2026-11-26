package com.example.nextclass.worker

import android.content.Context
import android.content.Intent
import com.example.nextclass.data.local.AppPreferences
import java.time.LocalDateTime

/** Arms the single evening alarm for the next planning prompt, or cancels it when turned off. */
object PlanReminderScheduler {

    fun schedule(context: Context, now: LocalDateTime = LocalDateTime.now()) {
        val appContext = context.applicationContext
        val prefs = AppPreferences(appContext)
        val intent = Intent(appContext, PlanReminderReceiver::class.java)
        val pending = AlarmHelper.pending(appContext, AlarmHelper.REQ_PLAN, intent)
        if (!prefs.planReminderEnabled) {
            AlarmHelper.cancel(appContext, pending)
            return
        }
        val minutes = prefs.planMinutes
        var trigger = now.toLocalDate().atTime(minutes / 60, minutes % 60)
        if (!trigger.isAfter(now.plusSeconds(30))) trigger = trigger.plusDays(1)
        AlarmHelper.schedule(appContext, AlarmHelper.millis(trigger), pending)
    }
}

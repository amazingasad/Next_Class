package com.example.nextclass.worker

import android.content.Context
import android.content.Intent
import com.example.nextclass.data.local.AppPreferences
import java.time.LocalDateTime

/** Arms the one alarm for the next weekly review, or cancels it when turned off. */
object WeeklyReviewScheduler {

    fun schedule(context: Context, now: LocalDateTime = LocalDateTime.now()) {
        val appContext = context.applicationContext
        val prefs = AppPreferences(appContext)
        val pending = AlarmHelper.pending(
            appContext,
            AlarmHelper.REQ_REVIEW,
            Intent(appContext, WeeklyReviewReceiver::class.java),
        )
        if (!prefs.reviewEnabled) {
            AlarmHelper.cancel(appContext, pending)
            return
        }
        val minutes = prefs.reviewMinutes
        var date = now.toLocalDate()
        var trigger = date.atTime(minutes / 60, minutes % 60)
        while (date.dayOfWeek.value != prefs.reviewDay || !trigger.isAfter(now.plusSeconds(30))) {
            date = date.plusDays(1)
            trigger = date.atTime(minutes / 60, minutes % 60)
        }
        AlarmHelper.schedule(appContext, AlarmHelper.millis(trigger), pending)
    }
}

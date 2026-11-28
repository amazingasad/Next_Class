package com.example.nextclass.worker

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.nextclass.MainActivity
import com.example.nextclass.R
import com.example.nextclass.data.local.AppDatabase
import com.example.nextclass.data.local.AppPreferences
import com.example.nextclass.data.local.toDomain
import com.example.nextclass.domain.model.WeeklyReview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Posts the weekly summary and arms the next one. */
class WeeklyReviewReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                post(appContext)
            } finally {
                WeeklyReviewScheduler.schedule(appContext)
                pending.finish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun post(context: Context) {
        if (!AppPreferences(context).reviewEnabled) return
        val db = AppDatabase.getInstance(context)
        val end = LocalDate.now()
        val start = end.minusDays(6)
        val routine = db.routineDao()
        val planner = db.plannerDao()
        val data = WeeklyReview.compute(
            end = end,
            sessions = routine.getAll().map { it.toDomain() },
            overrides = routine.getOverrides(start.toString()).map { it.toDomain() },
            breaks = routine.getBreaks().map { it.toDomain() },
            courses = routine.getCourses().map { it.toDomain() },
            marks = routine.getAttendance().map { it.toDomain() },
            tasks = planner.tasksBetween(start.toString(), end.toString()).map { it.toDomain() },
            study = planner.studyBetween(start.toString(), end.toString()).map { it.toDomain() },
        )

        NotificationHelper.ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled() || !NotificationHelper.hasPermission(context)) return
        val open = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_REVIEW)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_REVIEW)
            .setSmallIcon(R.drawable.ic_stat_class)
            .setContentTitle("Your week")
            .setContentText(data.summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(data.summary))
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()
        manager.notify(NotificationHelper.NOTIFICATION_ID_REVIEW, notification)
    }
}

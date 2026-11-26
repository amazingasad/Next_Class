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
import com.example.nextclass.widget.WidgetUpdateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * The evening "plan tomorrow" reminder. It stays quiet when tomorrow already has tasks, and it
 * re-arms the next day's alarm when it is done.
 */
class PlanReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                post(appContext)
                WidgetUpdateHelper.refreshAll(appContext)
            } finally {
                pending.finish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun post(context: Context) {
        if (!AppPreferences(context).planReminderEnabled) return
        val dao = AppDatabase.getInstance(context).plannerDao()
        val today = LocalDate.now()
        val tomorrowTasks = dao.tasksOn(today.plusDays(1).toString())
        if (tomorrowTasks.isNotEmpty()) return
        val unfinished = dao.tasksOn(today.toString()).count { !it.done }

        NotificationHelper.ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled() || !NotificationHelper.hasPermission(context)) return

        val open = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_PLAN)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val body = when (unfinished) {
            0 -> "Add your tasks for tomorrow."
            1 -> "1 task is not finished. Add tomorrow's tasks and keep it if you want."
            else -> "$unfinished tasks are not finished. Add tomorrow's tasks and keep any you want."
        }
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_PLAN)
            .setSmallIcon(R.drawable.ic_stat_class)
            .setContentTitle("Plan tomorrow")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()
        manager.notify(NotificationHelper.NOTIFICATION_ID_PLAN, notification)
    }
}

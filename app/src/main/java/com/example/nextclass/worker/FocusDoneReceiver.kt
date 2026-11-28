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
import com.example.nextclass.domain.model.TimeFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires when the study timer reaches zero: logs the session and tells the user. */
class FocusDoneReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val session = FocusManager.load(appContext)
                val minutes = FocusManager.finish(appContext)
                if (session != null && minutes > 0) post(appContext, session.courseCode, minutes)
            } finally {
                pending.finish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun post(context: Context, code: String, minutes: Int) {
        NotificationHelper.ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled() || !NotificationHelper.hasPermission(context)) return
        val open = PendingIntent.getActivity(
            context,
            3,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_FOCUS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val body = "$code, " + TimeFormat.duration(minutes) + " logged."
        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_stat_class)
            .setContentTitle("Study session finished")
            .setContentText(body)
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        manager.notify(NotificationHelper.NOTIFICATION_ID_FOCUS, notification)
    }
}

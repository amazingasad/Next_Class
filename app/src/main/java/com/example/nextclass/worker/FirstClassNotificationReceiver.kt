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

/** Posts the single daily "first class" notification. Nothing else ever posts one. */
class FirstClassNotificationReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val code = intent.getStringExtra(EXTRA_CODE) ?: return
        val room = intent.getStringExtra(EXTRA_ROOM).orEmpty()
        val start = intent.getStringExtra(EXTRA_START) ?: return
        val lead = intent.getIntExtra(EXTRA_LEAD, 30)

        NotificationHelper.ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled() || !NotificationHelper.hasPermission(context)) return

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val body = buildString {
            if (room.isNotBlank()) append(room).append(" • ")
            append("Starts at ").append(TimeFormat.clock12(start))
        }

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_FIRST_CLASS)
            .setSmallIcon(R.drawable.ic_stat_class)
            .setContentTitle("First Class in ${lead}m: $code")
            .setContentText(body)
            .setContentIntent(openApp)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(NotificationHelper.NOTIFICATION_ID_FIRST_CLASS, notification)
    }

    companion object {
        const val EXTRA_CODE = "extra_code"
        const val EXTRA_ROOM = "extra_room"
        const val EXTRA_START = "extra_start"
        const val EXTRA_LEAD = "extra_lead"
    }
}

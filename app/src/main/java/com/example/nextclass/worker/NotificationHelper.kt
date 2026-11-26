package com.example.nextclass.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.nextclass.R

object NotificationHelper {

    const val CHANNEL_FIRST_CLASS = "first_class"
    const val NOTIFICATION_ID_FIRST_CLASS = 2001
    const val CHANNEL_PLAN = "plan_tomorrow"
    const val NOTIFICATION_ID_PLAN = 2002

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_FIRST_CLASS,
            context.getString(R.string.channel_first_class_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_first_class_description)
        }
        manager.createNotificationChannel(channel)
        val plan = NotificationChannel(
            CHANNEL_PLAN,
            context.getString(R.string.channel_plan_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_plan_description)
        }
        manager.createNotificationChannel(plan)
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}

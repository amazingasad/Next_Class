package com.example.nextclass.data.local

import android.content.Context
import android.content.SharedPreferences

/** Non-sensitive settings. The API key lives in [ApiKeyManager] instead. */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var leadMinutes: Int
        get() = prefs.getInt(KEY_LEAD_MINUTES, DEFAULT_LEAD_MINUTES)
        set(value) {
            prefs.edit().putInt(KEY_LEAD_MINUTES, value).apply()
        }

    var firstClassReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDER_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_REMINDER_ENABLED, value).apply()
        }

    companion object {
        const val DEFAULT_LEAD_MINUTES = 30
        val LEAD_OPTIONS = listOf(15, 30, 45)

        private const val FILE = "nextclass_prefs"
        private const val KEY_LEAD_MINUTES = "lead_minutes"
        private const val KEY_REMINDER_ENABLED = "first_class_reminder_enabled"
    }
}

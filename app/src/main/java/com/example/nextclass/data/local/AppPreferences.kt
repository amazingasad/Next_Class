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

    /** 0 follows the system, 1 is always light, 2 is always dark. */
    var widgetTheme: Int
        get() = prefs.getInt(KEY_WIDGET_THEME, 0)
        set(value) {
            prefs.edit().putInt(KEY_WIDGET_THEME, value).apply()
        }

    var planReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_PLAN_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_PLAN_ENABLED, value).apply()
        }

    /** Minutes after midnight when the "plan tomorrow" reminder fires. Default 9:00 PM. */
    var planMinutes: Int
        get() = prefs.getInt(KEY_PLAN_MINUTES, DEFAULT_PLAN_MINUTES)
        set(value) {
            prefs.edit().putInt(KEY_PLAN_MINUTES, value).apply()
        }

    /** 0 shows the note page of the planner widget, 1 shows the tasks page. */
    var plannerPage: Int
        get() = prefs.getInt(KEY_PLANNER_PAGE, 0)
        set(value) {
            prefs.edit().putInt(KEY_PLANNER_PAGE, value).apply()
        }

    var dynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC, false)
        set(value) {
            prefs.edit().putBoolean(KEY_DYNAMIC, value).apply()
        }

    var batteryNoticeHidden: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_HIDDEN, false)
        set(value) {
            prefs.edit().putBoolean(KEY_BATTERY_HIDDEN, value).apply()
        }

    var lastUpdateCheck: Long
        get() = prefs.getLong(KEY_LAST_UPDATE, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_UPDATE, value).apply()
        }

    companion object {
        const val DEFAULT_LEAD_MINUTES = 30
        const val DEFAULT_PLAN_MINUTES = 21 * 60
        val PLAN_OPTIONS = listOf(19 * 60, 20 * 60, 21 * 60, 22 * 60)
        val LEAD_OPTIONS = listOf(15, 30, 45)

        private const val FILE = "nextclass_prefs"
        private const val KEY_PLAN_ENABLED = "plan_reminder_enabled"
        private const val KEY_PLAN_MINUTES = "plan_minutes"
        private const val KEY_PLANNER_PAGE = "planner_page"
        private const val KEY_DYNAMIC = "dynamic_color"
        private const val KEY_BATTERY_HIDDEN = "battery_notice_hidden"
        private const val KEY_LAST_UPDATE = "last_update_check"
        private const val KEY_WIDGET_THEME = "widget_theme"
        private const val KEY_LEAD_MINUTES = "lead_minutes"
        private const val KEY_REMINDER_ENABLED = "first_class_reminder_enabled"
    }
}

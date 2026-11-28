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

    var reviewEnabled: Boolean
        get() = prefs.getBoolean(KEY_REVIEW_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_REVIEW_ENABLED, value).apply()
        }

    /** Day of the week, 1 = Monday, when the weekly review notification arrives. Default Friday. */
    var reviewDay: Int
        get() = prefs.getInt(KEY_REVIEW_DAY, 5)
        set(value) {
            prefs.edit().putInt(KEY_REVIEW_DAY, value).apply()
        }

    /** Minutes after midnight for the weekly review. Default 6:00 PM. */
    var reviewMinutes: Int
        get() = prefs.getInt(KEY_REVIEW_MINUTES, 18 * 60)
        set(value) {
            prefs.edit().putInt(KEY_REVIEW_MINUTES, value).apply()
        }

    var focusCourseKey: String
        get() = prefs.getString(KEY_FOCUS_KEY, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_FOCUS_KEY, value).apply()
        }

    var focusCourseCode: String
        get() = prefs.getString(KEY_FOCUS_CODE, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_FOCUS_CODE, value).apply()
        }

    var focusTotalSec: Int
        get() = prefs.getInt(KEY_FOCUS_TOTAL, 0)
        set(value) {
            prefs.edit().putInt(KEY_FOCUS_TOTAL, value).apply()
        }

    var focusEndAt: Long
        get() = prefs.getLong(KEY_FOCUS_END, 0L)
        set(value) {
            prefs.edit().putLong(KEY_FOCUS_END, value).apply()
        }

    /** Seconds left while paused, or -1 while the timer is running. */
    var focusPausedLeft: Int
        get() = prefs.getInt(KEY_FOCUS_PAUSED, -1)
        set(value) {
            prefs.edit().putInt(KEY_FOCUS_PAUSED, value).apply()
        }

    /** Writes every focus field in one commit so a reader never sees half a session. */
    fun writeFocus(key: String, code: String, total: Int, endAt: Long, pausedLeft: Int) {
        prefs.edit()
            .putString(KEY_FOCUS_KEY, key)
            .putString(KEY_FOCUS_CODE, code)
            .putInt(KEY_FOCUS_TOTAL, total)
            .putLong(KEY_FOCUS_END, endAt)
            .putInt(KEY_FOCUS_PAUSED, pausedLeft)
            .commit()
    }

    /** 0 chooses automatically, 1 always reads offline, 2 always uses Gemini. */
    var scanMode: Int
        get() = prefs.getInt(KEY_SCAN_MODE, 0)
        set(value) {
            prefs.edit().putInt(KEY_SCAN_MODE, value).apply()
        }

    /** 0 follows the phone, 1 is always light, 2 is always dark. Applies to the whole app. */
    var appThemeMode: Int
        get() = prefs.getInt(KEY_APP_THEME, 0)
        set(value) {
            prefs.edit().putInt(KEY_APP_THEME, value).apply()
        }

    /** Index into the palette list: 0 Paper, 1 Forest, 2 Harbor. */
    var paletteId: Int
        get() = prefs.getInt(KEY_PALETTE, 0)
        set(value) {
            prefs.edit().putInt(KEY_PALETTE, value).apply()
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

        private const val KEY_SCAN_MODE = "scan_mode"
        private const val KEY_APP_THEME = "app_theme_mode"
        private const val KEY_PALETTE = "palette_id"
        private const val KEY_REVIEW_ENABLED = "review_enabled"
        private const val KEY_REVIEW_DAY = "review_day"
        private const val KEY_REVIEW_MINUTES = "review_minutes"
        private const val KEY_FOCUS_KEY = "focus_key"
        private const val KEY_FOCUS_CODE = "focus_code"
        private const val KEY_FOCUS_TOTAL = "focus_total"
        private const val KEY_FOCUS_END = "focus_end"
        private const val KEY_FOCUS_PAUSED = "focus_paused"
        val REVIEW_DAY_OPTIONS = listOf(4, 5, 6, 7)
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

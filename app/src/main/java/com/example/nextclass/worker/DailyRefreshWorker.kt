package com.example.nextclass.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nextclass.widget.WidgetUpdateHelper

/**
 * A once-a-day safety net in case the system dropped an alarm. This is not polling: the widget
 * itself is driven by exact alarms.
 */
class DailyRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        WidgetUpdateHelper.refreshAll(applicationContext)
        return Result.success()
    }
}

package com.example.nextclass

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.nextclass.worker.DailyRefreshWorker
import com.example.nextclass.worker.NotificationHelper
import java.util.concurrent.TimeUnit

class NextClassApp : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_refresh",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DailyRefreshWorker>(24, TimeUnit.HOURS).build(),
        )
    }
}

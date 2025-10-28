package com.example.reminderapp

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.reminderapp.work.CalendarSyncWorker
import java.util.concurrent.TimeUnit

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // Periodic sync every 3 hours, flex 15 minutes
        val request = PeriodicWorkRequestBuilder<CalendarSyncWorker>(3, TimeUnit.HOURS)
            .setInitialDelay(5, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "calendar_sync",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}



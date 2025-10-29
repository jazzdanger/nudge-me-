package com.example.reminderapp.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.util.Log
import androidx.core.app.NotificationCompat
import android.app.NotificationManager
import android.content.Intent
import java.lang.Exception
import androidx.core.content.ContextCompat
import com.example.reminderapp.AlarmReciver

class HydrationWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "HydrationWorker"
    }

    override suspend fun doWork(): Result {
        try {
            Log.d(TAG, "HydrationWorker triggered - sending hydration notification")

            // Reuse AlarmReciver's notification logic by calling its helper
            val receiver = AlarmReciver()
            receiver.onReceive(applicationContext, Intent())

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in HydrationWorker: ${e.message}")
            return Result.failure()
        }
    }
}

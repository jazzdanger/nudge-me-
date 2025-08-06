package com.example.reminderapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmReciver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val CHANNEL_ID = "ReminderChannel"
        private const val CHANNEL_NAME = "Reminder Notifications"
        private const val CHANNEL_DESCRIPTION = "Channel for reminder notifications"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm received")
        
        try {
            // Retrieve the reminder details from the intent
            val title = intent.getStringExtra("title") ?: "Reminder"
            val notes = intent.getStringExtra("notes") ?: ""

            Log.d(TAG, "Showing notification for: $title")
            
            // Show the notification
            showNotification(context, title, notes)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onReceive: ${e.message}")
            // Fallback notification if something goes wrong
            showNotification(context, "Reminder", "You have a reminder!")
        }
    }

    private fun showNotification(context: Context, title: String, notes: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Generate unique notification ID based on current time
            val notificationId = System.currentTimeMillis().toInt()
            
            Log.d(TAG, "Creating notification with ID: $notificationId")

            // Create notification channel for Android 8.0+
            createNotificationChannel(notificationManager)

            // Build the notification
            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(if (notes.isNotEmpty()) notes else "Time for your reminder!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000)) // Vibration pattern
                .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
                .setOnlyAlertOnce(false) // Allow multiple alerts
                .setOngoing(false) // Not persistent

            // Display the notification
            notificationManager.notify(notificationId, notificationBuilder.build())
            
            Log.d(TAG, "Notification sent successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification: ${e.message}")
            // If notification fails, try to show a basic one
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                createNotificationChannel(notificationManager)
                
                val basicNotification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("Reminder")
                    .setContentText("You have a reminder!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
                    .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
                    .build()
                
                notificationManager.notify(1, basicNotification)
                Log.d(TAG, "Fallback notification sent")
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to send fallback notification: ${ex.message}")
                ex.printStackTrace()
            }
        }
    }
    
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            
            try {
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channel: ${e.message}")
            }
        }
    }
}
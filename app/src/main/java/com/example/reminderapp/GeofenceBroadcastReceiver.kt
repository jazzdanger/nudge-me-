package com.example.reminderapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    
    companion object {
        private const val CHANNEL_ID = "GeofenceChannel"
        private const val CHANNEL_NAME = "Location-based Reminders"
        private const val CHANNEL_DESCRIPTION = "Notifications triggered by location events"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            if (geofencingEvent?.hasError() == true) {
                val errorCode = geofencingEvent.errorCode
                Log.e("GeofenceReceiver", "Geofencing error: $errorCode")
                return
            }
            
            val geofenceTransition = geofencingEvent?.geofenceTransition
            val triggeringGeofences = geofencingEvent?.triggeringGeofences
            
            val title = intent.getStringExtra("title") ?: "Reminder"
            val notes = intent.getStringExtra("notes") ?: ""
            val triggerType = intent.getStringExtra("trigger_type") ?: "ENTER"
            
            // Use notes if available, otherwise use a simple default message
            val message = if (notes.isNotEmpty()) notes else "Time for your reminder!"
            
            Log.d("GeofenceReceiver", "Transition: $geofenceTransition, Trigger Type: $triggerType, Title: $title, Notes: $notes")
            
            showNotification(context, title, message)
            
        } catch (e: Exception) {
            Log.e("GeofenceReceiver", "Error showing geofence notification: ${e.message}")
            // Fallback notification
            showNotification(context, "Reminder", "You have a reminder!")
        }
    }
    
    private fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android 8.0+
        createNotificationChannel(notificationManager)
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
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
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}



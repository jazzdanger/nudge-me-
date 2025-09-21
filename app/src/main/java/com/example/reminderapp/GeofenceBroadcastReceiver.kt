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
            val triggerType = intent.getStringExtra("trigger_type") ?: "ENTER"
            
            // Determine message based on actual geofence transition and intended trigger type
            val message = when {
                geofenceTransition == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER && triggerType == "ENTER" -> 
                    "You have arrived at your selected location"
                geofenceTransition == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT && triggerType == "LEAVE" -> 
                    "You have left your selected location"
                geofenceTransition == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_DWELL && triggerType == "AT" -> 
                    "You are currently at your selected location"
                geofenceTransition == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT && triggerType == "NOT_AT" -> 
                    "You are not at your selected location"
                else -> {
                    // Fallback based on transition type
                    when (geofenceTransition) {
                        com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER -> "You have entered the location"
                        com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT -> "You have left the location"
                        com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_DWELL -> "You are dwelling at the location"
                        else -> "Location-based reminder triggered"
                    }
                }
            }
            
            Log.d("GeofenceReceiver", "Transition: $geofenceTransition, Trigger Type: $triggerType, Message: $message")
            
            showNotification(context, title, message)
            
        } catch (e: Exception) {
            Log.e("GeofenceReceiver", "Error showing geofence notification: ${e.message}")
            // Fallback notification
            showNotification(context, "Reminder", "Location-based reminder triggered")
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



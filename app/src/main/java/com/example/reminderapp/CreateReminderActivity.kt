package com.example.reminderapp

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.*

class CreateReminderActivity : AppCompatActivity() {
    private lateinit var editTextTitle: EditText
    private lateinit var editTextDate: EditText
    private lateinit var editTextTime: EditText
    private lateinit var editTextNotes: EditText
    private lateinit var switchRepeat: SwitchMaterial
    private lateinit var buttonSetReminder: Button
    private lateinit var backButton: ImageView
    
    private var selectedDate: Calendar? = null
    private var selectedTime: Calendar? = null
    private val CHANNEL_ID = "ReminderChannel"
    
    companion object {
        private const val TAG = "CreateReminderActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_reminder)
        
        createNotificationChannel()
        initializeViews()
        setupClickListeners()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Reminder Notifications"
            val descriptionText = "Channel for reminder notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }
    
    private fun initializeViews() {
        editTextTitle = findViewById(R.id.editTextTitle)
        editTextDate = findViewById(R.id.editTextDate)
        editTextTime = findViewById(R.id.editTextTime)
        editTextNotes = findViewById(R.id.editTextNotes)
        switchRepeat = findViewById(R.id.switchRepeat)
        buttonSetReminder = findViewById(R.id.buttonSetReminder)
        backButton = findViewById(R.id.backButton)
    }
    
    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }
        
        editTextDate.setOnClickListener {
            showDatePicker()
        }
        
        editTextTime.setOnClickListener {
            showTimePicker()
        }
        
        buttonSetReminder.setOnClickListener {
            saveReminder()
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                editTextDate.setText(dateFormat.format(selectedDate?.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    
    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                editTextTime.setText(timeFormat.format(selectedTime?.time))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }
    
    private fun saveReminder() {
        val title = editTextTitle.text.toString().trim()
        val notes = editTextNotes.text.toString().trim()
        
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedDate == null) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedTime == null) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            // Format the date and time for display
            val displayDateTime = formatDateTimeForDisplay()
            
            // Add reminder to the main list
            MainActivity.addReminder(title, displayDateTime)
            
            // Schedule the alarm
            val scheduledTime = scheduleAlarm(title, notes)
            
            val message = "Reminder set: $title at $displayDateTime"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            
            Log.d(TAG, "Reminder scheduled for: $scheduledTime")
            
            // Return to main activity
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting reminder: ${e.message}")
            Toast.makeText(this, "Error setting reminder: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun formatDateTimeForDisplay(): String {
        val now = Calendar.getInstance()
        val reminderDate = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedDate!!.get(Calendar.YEAR))
            set(Calendar.MONTH, selectedDate!!.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, selectedDate!!.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, selectedTime!!.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, selectedTime!!.get(Calendar.MINUTE))
        }
        
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeString = timeFormat.format(selectedTime!!.time)
        
        return when {
            // Today
            reminderDate.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            reminderDate.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) -> {
                "Today, $timeString"
            }
            // Tomorrow
            reminderDate.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            reminderDate.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) + 1 -> {
                "Tomorrow, $timeString"
            }
            // This week
            reminderDate.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            reminderDate.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR) -> {
                val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                "${dayFormat.format(reminderDate.time)}, $timeString"
            }
            // Other dates
            else -> {
                val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                "${dateFormat.format(reminderDate.time)}, $timeString"
            }
        }
    }
    
    private fun scheduleAlarm(title: String, notes: String): String {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReciver::class.java).apply {
            putExtra("title", title)
            putExtra("notes", notes)
        }
        
        // Generate unique ID for each reminder
        val uniqueId = System.currentTimeMillis().toInt()
        
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            uniqueId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Combine date and time properly
        val reminderTime = Calendar.getInstance().apply {
            // Set the date
            set(Calendar.YEAR, selectedDate!!.get(Calendar.YEAR))
            set(Calendar.MONTH, selectedDate!!.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, selectedDate!!.get(Calendar.DAY_OF_MONTH))
            
            // Set the time
            set(Calendar.HOUR_OF_DAY, selectedTime!!.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, selectedTime!!.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // If the time has already passed today, schedule for tomorrow
        if (reminderTime.timeInMillis <= System.currentTimeMillis()) {
            reminderTime.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val scheduledTimeString = timeFormat.format(reminderTime.time)
        
        Log.d(TAG, "Scheduling alarm for: $scheduledTimeString")
        
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime.timeInMillis,
                pendingIntent
            )
            Log.d(TAG, "Alarm scheduled successfully with exact timing")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm not allowed, using regular alarm")
            // Handle case where exact alarms are not allowed
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminderTime.timeInMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.message}")
            throw e
        }
        
        return scheduledTimeString
    }
} 
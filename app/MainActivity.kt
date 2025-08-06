package com.example.reminderapp

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var editTextReminder: EditText
    private lateinit var buttonSetTime: Button
    private lateinit var buttonSaveReminder: Button

    private var reminderTime: Calendar = Calendar.getInstance()

    // Request notification permission for Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted. You can proceed with notifications.
            Toast.makeText(this, "Notification permission granted.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notification permission is required to show reminders.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request notification permission as soon as the app starts
        askNotificationPermission()

        // Initialize UI components from the layout file
        editTextReminder = findViewById(R.id.editTextReminder)
        buttonSetTime = findViewById(R.id.buttonSetTime)
        buttonSaveReminder = findViewById(R.id.buttonSaveReminder)

        // Set a click listener for the "Set Time" button
        buttonSetTime.setOnClickListener {
            showDateTimePicker()
        }

        // Set a click listener for the "Save Reminder" button
        buttonSaveReminder.setOnClickListener {
            val reminderText = editTextReminder.text.toString()
            if (reminderText.isNotBlank()) {
                scheduleReminder(reminderText)
            } else {
                Toast.makeText(this, "Please enter a reminder message.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun askNotificationPermission() {
        // This is only required for Android 13 (API 33) and higher.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun showDateTimePicker() {
        val currentDate = Calendar.getInstance()
        // Show Date Picker Dialog
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            reminderTime.set(Calendar.YEAR, year)
            reminderTime.set(Calendar.MONTH, month)
            reminderTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            // After picking the date, show the Time Picker Dialog
            TimePickerDialog(this, { _, hourOfDay, minute ->
                reminderTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                reminderTime.set(Calendar.MINUTE, minute)
                reminderTime.set(Calendar.SECOND, 0)

                // Update the button's text to show the selected date and time
                val formattedTime = android.text.format.DateFormat.format("hh:mm a, dd/MM/yy", reminderTime)
                buttonSetTime.text = "Time: $formattedTime"

            }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), false).show()

        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun scheduleReminder(message: String) {
        val intent = Intent(this, AlarmReceiver::class.java)
        intent.putExtra("REMINDER_MESSAGE", message)

        // A PendingIntent is a token that you give to a foreign application
        // (e.g., AlarmManager), which allows the foreign application to use
        // your application's permissions to execute a predefined piece of code.
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            1, // A unique request code for this reminder
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check if the app has permission to schedule exact alarms (required for Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                // Schedule the exact alarm
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime.timeInMillis,
                    pendingIntent
                )
                Toast.makeText(this, "Reminder set successfully!", Toast.LENGTH_SHORT).show()
            } else {
                // Inform user and optionally direct them to settings
                Toast.makeText(this, "Permission to schedule exact alarms is required.", Toast.LENGTH_LONG).show()
                // Optionally, open settings for the user to grant permission:
                // startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        } else {
            // For older Android versions, schedule the exact alarm directly
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime.timeInMillis,
                pendingIntent
            )
            Toast.makeText(this, "Reminder set successfully!", Toast.LENGTH_SHORT).show()
        }
    }
}
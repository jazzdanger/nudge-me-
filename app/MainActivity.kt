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
import androidx.activity.result.ActivityResultLauncher
import android.Manifest
import android.provider.Settings
import android.net.Uri
import androidx.appcompat.app.AlertDialog
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

    // Foreground location (fine/coarse) launcher
    private val requestForegroundLocationLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val granted = results.entries.any { it.value }
            if (granted) {
                // If API >= Q, request background location
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestBackgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            } else {
                Toast.makeText(this, "Location permission denied. Some features may not work.", Toast.LENGTH_LONG).show()
            }
        }

    // Background location launcher
    private val requestBackgroundLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                Toast.makeText(this, "Background location granted", Toast.LENGTH_SHORT).show()
            } else {
                // Guide user to settings for "All the time"
                AlertDialog.Builder(this)
                    .setTitle("Allow all the time")
                    .setMessage("To receive location reminders while the app is not running, please allow 'All the time' in App settings.")
                    .setPositiveButton("Open settings") { _, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.fromParts("package", packageName, null)
                        startActivity(intent)
                    }
                    .setNegativeButton("Not now", null)
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request notification permission as soon as the app starts
        askNotificationPermission()
        // Also request location permissions (foreground then background) at first run
        askLocationPermissionsAtStartup()

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

    private fun askLocationPermissionsAtStartup() {
        // If foreground location not granted, request it. If granted, immediately request background when supported.
        val fine = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) else android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) else android.content.pm.PackageManager.PERMISSION_GRANTED

        if (fine != android.content.pm.PackageManager.PERMISSION_GRANTED && coarse != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // Request foreground permissions (fine/coarse) together
            requestForegroundLocationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            // Foreground already granted, request background if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val background = checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                if (background != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    // Show brief rationale then request
                    AlertDialog.Builder(this)
                        .setTitle("Background location")
                        .setMessage("Allowing background location lets the app trigger location reminders even when the app isn't open. Please allow 'All the time' on the next screen.")
                        .setPositiveButton("Continue") { _, _ ->
                            requestBackgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                        .setNegativeButton("Not now", null)
                        .show()
                }
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
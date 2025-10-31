package com.example.reminderapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reminderapp.SettingItem
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class SettingsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SettingsAdapter
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        recyclerView = findViewById(R.id.settingsRecyclerView)

        val prefs: SharedPreferences = getSharedPreferences("settings_prefs", MODE_PRIVATE)
        val waterEnabled = prefs.getBoolean("water_reminder_enabled", false)
        val settingsList = listOf(
            SettingItem.Toggle(
                title = "Water Reminders",
                description = "Get a reminder to hydrate every 2 hours",
                enabled = waterEnabled,
                onToggle = { enabled ->
                    prefs.edit().putBoolean("water_reminder_enabled", enabled).apply()
                    if (enabled) scheduleHydrationWorker() else cancelHydrationWorker()
                    Toast.makeText(this, if (enabled) "Water reminders enabled" else "Water reminders disabled", Toast.LENGTH_SHORT).show()
                }
            )
        )

        adapter = SettingsAdapter(settingsList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Set up sign out button
        val signoutButton: Button = findViewById(R.id.signoutButton)
        signoutButton.setOnClickListener {
            signOut()
        }
    }

    private fun handleSettingClick(item: String) {
        when (item) {
            "Water Reminders" -> {
                // Show toggle dialog for water reminders
                showWaterReminderToggle()
            }
            "Edit Profile" -> {
                Toast.makeText(this, "Edit Profile feature coming soon!", Toast.LENGTH_SHORT).show()
            }
            "Change Password" -> {
                Toast.makeText(this, "Change Password feature coming soon!", Toast.LENGTH_SHORT).show()
            }
            "Notifications" -> {
                Toast.makeText(this, "Notification settings coming soon!", Toast.LENGTH_SHORT).show()
            }
            "Privacy & Security" -> {
                Toast.makeText(this, "Privacy settings coming soon!", Toast.LENGTH_SHORT).show()
            }
            "Language & Region" -> {
                Toast.makeText(this, "Language settings coming soon!", Toast.LENGTH_SHORT).show()
            }
            "Help & FAQs" -> {
                Toast.makeText(this, "Help section coming soon!", Toast.LENGTH_SHORT).show()
            }
            "Logout" -> {
                Toast.makeText(this, "Logout feature coming soon!", Toast.LENGTH_SHORT).show()
                // TODO: Add your logout logic here
            }
        }
    }

    private fun showWaterReminderToggle() {
        val prefs: SharedPreferences = getSharedPreferences("settings_prefs", MODE_PRIVATE)
        val currentlyEnabled = prefs.getBoolean("water_reminder_enabled", false)

        val inflater = LayoutInflater.from(this)
        val toggle = Switch(this)
        toggle.isChecked = currentlyEnabled
        toggle.text = "Enable every 2 hours"

        val dialog = AlertDialog.Builder(this)
            .setTitle("Water Reminders")
            .setMessage("Get a reminder to hydrate every 2 hours")
            .setView(toggle)
            .setPositiveButton("OK") { _, _ ->
                val enabled = toggle.isChecked
                prefs.edit().putBoolean("water_reminder_enabled", enabled).apply()
                if (enabled) scheduleHydrationWorker()
                else cancelHydrationWorker()
                Toast.makeText(this, if (enabled) "Water reminders enabled" else "Water reminders disabled", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()

        // Allow immediate toggle changes
        toggle.setOnCheckedChangeListener { _: CompoundButton, _: Boolean -> }
        dialog.show()
    }

    private fun scheduleHydrationWorker() {
        val request = PeriodicWorkRequestBuilder<com.example.reminderapp.work.HydrationWorker>(2, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "hydration_reminder",
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    private fun cancelHydrationWorker() {
        WorkManager.getInstance(this).cancelUniqueWork("hydration_reminder")
    }

    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener(this) {
            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
            // Optionally, navigate back to login or main screen
            finish()
        }
    }
}

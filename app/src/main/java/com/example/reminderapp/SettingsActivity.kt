package com.example.reminderapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SettingsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SettingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        recyclerView = findViewById(R.id.settingsRecyclerView)

        val settingsList = listOf(
            "Edit Profile",
            "Change Password",
            "Notifications",
            "Privacy & Security",
            "Language & Region",
            "Help & FAQs",
            "Logout"
        )

        adapter = SettingsAdapter(settingsList) { selectedItem ->
            handleSettingClick(selectedItem)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun handleSettingClick(item: String) {
        when (item) {
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
}

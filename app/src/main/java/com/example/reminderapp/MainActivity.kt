package com.example.reminderapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.reminderapp.fragments.CalendarFragment
import com.example.reminderapp.fragments.HistoryFragment
import com.example.reminderapp.fragments.HomeFragment
import com.example.reminderapp.fragments.StatsFragment
import com.google.android.material.navigation.NavigationView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.reminderapp.work.CalendarSyncWorker

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var profileButton: ImageView
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        createNotificationChannel()
        setupPermissionLauncher()
        initializeViews()
        setupClickListeners()
        setupBottomNavigation()
        requestFirstLaunchPermissionsIfNeeded()

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        // Handle window insets for edge-to-edge display without double bottom padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply top/side insets; leave bottom to BottomNavigationView which already accounts for nav bar
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        Log.d("MainActivity", "MainActivity created")
    }

    private fun setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val denied = results.filterValues { granted -> !granted }.keys
            if (denied.isEmpty()) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions denied: ${denied.joinToString()}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestFirstLaunchPermissionsIfNeeded() {
        val prefs = getSharedPreferences("onboarding", Context.MODE_PRIVATE)
        val asked = prefs.getBoolean("permissions_requested", false)
        if (asked) return

        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(android.Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(android.Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)

        permissionLauncher.launch(permissions.toTypedArray())
        prefs.edit().putBoolean("permissions_requested", true).apply()
    }



    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Reminder Notifications"
            val descriptionText = "Channel for reminder notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("ReminderChannel", name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("MainActivity", "Notification channel created")
        }
    }

    private fun initializeViews() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
        profileButton = findViewById(R.id.profileButton)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        Log.d("MainActivity", "Views initialized")
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun setupClickListeners() {
        profileButton.setOnClickListener {
            drawerLayout.open()
        }

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_profile -> Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show()
                R.id.menu_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                }
                R.id.menu_notifications -> Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show()
                R.id.menu_about -> Toast.makeText(this, "About", Toast.LENGTH_SHORT).show()
            }
            drawerLayout.close()
            true
        }
    }

    private fun showTestNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = System.currentTimeMillis().toInt()

            val notification = NotificationCompat.Builder(this, "ReminderChannel")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Test Reminder")
                .setContentText("This is a test notification from your reminder app!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
                .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
                .build()

            notificationManager.notify(notificationId, notification)
            Log.d("MainActivity", "Test notification sent")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error sending test notification: ${e.message}")
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_today -> {
                    loadFragment(HistoryFragment())
                    true
                }
                R.id.nav_calendar -> {
                    // Trigger an immediate calendar sync when Calendar tab is selected
                    WorkManager.getInstance(this).enqueue(
                        OneTimeWorkRequestBuilder<CalendarSyncWorker>().build()
                    )
                    loadFragment(CalendarFragment())
                    true
                }
                R.id.nav_stats -> {
                    loadFragment(StatsFragment())
                    true
                }
                else -> false
            }
        }
    }


}

data class Reminder(
    val id: Long = 0,
    val title: String,
    val dateTime: String,
    val iconResId: Int,
    val status: ReminderStatus,
    val repeatDays: Set<Int> = emptySet(),
    val isCompleted: Boolean = false,
    val notes: String? = null,
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val locationTriggerType: String? = null
)


enum class ReminderStatus {
    PENDING,
    ACTIVE,
    COMPLETED
}
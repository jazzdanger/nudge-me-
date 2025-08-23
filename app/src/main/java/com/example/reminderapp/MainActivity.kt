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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.drawerlayout.widget.DrawerLayout
import com.example.reminderapp.data.ReminderDatabase
import com.example.reminderapp.data.ReminderRepository
import com.google.android.material.navigation.NavigationView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

    class MainActivity : AppCompatActivity() {
    private lateinit var recyclerViewReminders: RecyclerView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var fabAddReminder: FloatingActionButton
    private lateinit var profileButton: ImageView
    // Removed notificationButton as per requirement
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var reminderAdapter: ReminderAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var reminderRepository: ReminderRepository

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
        enableEdgeToEdge()
            setContentView(R.layout.activity_main)

        // Initialize database
        val database = ReminderDatabase.getDatabase(this)
        reminderRepository = ReminderRepository(database.reminderDao())

        createNotificationChannel()
        setupPermissionLauncher()
        initializeViews()
        setupRecyclerView() // Setup adapter first
        setupClickListeners()
        setupBottomNavigation()
        requestFirstLaunchPermissionsIfNeeded()
        
        // Observe reminders from database
        observeReminders()
        
        // Handle window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        Log.d("MainActivity", "MainActivity created")
        
        // No test reminders - start with empty list
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
        recyclerViewReminders = findViewById(R.id.recyclerViewReminders)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        fabAddReminder = findViewById(R.id.fabAddReminder)
        profileButton = findViewById(R.id.profileButton)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        // notificationButton removed from layout
        Log.d("MainActivity", "Views initialized")
    }
    
    private fun setupRecyclerView() {
        Log.d("MainActivity", "Setting up RecyclerView")
        
        reminderAdapter = ReminderAdapter(emptyList()) { reminder ->
            // Handle reminder deletion
            lifecycleScope.launch {
                reminderRepository.deleteReminder(reminder)
                Toast.makeText(this@MainActivity, "Reminder deleted: ${reminder.title}", Toast.LENGTH_SHORT).show()
            }
        }
        recyclerViewReminders.layoutManager = LinearLayoutManager(this)
        recyclerViewReminders.adapter = reminderAdapter
        
        Log.d("MainActivity", "RecyclerView setup complete")
    }
    
    private fun observeReminders() {
        lifecycleScope.launch {
            reminderRepository.allReminders.collectLatest { reminders ->
                Log.d("MainActivity", "Observed ${reminders.size} reminders from database")
                reminderAdapter.updateReminders(reminders)
            }
        }
    }
    
    private fun setupClickListeners() {
        fabAddReminder.setOnClickListener {
            Log.d("MainActivity", "FAB clicked, launching CreateReminderActivity")
            val intent = Intent(this, CreateReminderActivity::class.java)
            startActivity(intent)
        }
        
        profileButton.setOnClickListener {
            drawerLayout.open()
        }
        
        // notificationButton click listener removed
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_profile -> Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show()
                R.id.menu_settings -> Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
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
                    // Handle Home tab - show all reminders
                    true
                }
                R.id.nav_today -> {
                    // Handle Today tab - show today's reminders
                    true
                }
                R.id.nav_calendar -> {
                    // Handle Calendar tab - show calendar view
                    true
                }
                R.id.nav_stats -> {
                    // Handle Stats tab - show statistics
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
    val status: ReminderStatus
)

enum class ReminderStatus {
    PENDING,
    ACTIVE,
    COMPLETED
    }
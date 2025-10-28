package com.example.reminderapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reminderapp.data.ReminderDatabase
import com.example.reminderapp.data.ReminderEntity
import com.example.reminderapp.data.ReminderRepository
import com.example.reminderapp.ReminderStatus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerViewHistory: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var reminderRepository: ReminderRepository


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // RecyclerView
        recyclerViewHistory = findViewById(R.id.recyclerViewReminders)

        // Repository
        val database = ReminderDatabase.getDatabase(applicationContext)
        reminderRepository = ReminderRepository(database.reminderDao())

        setupRecyclerView()
        observeCompletedReminders()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            onReminderRestoreClicked = { reminderEntity -> restoreReminderToActiveList(reminderEntity) },
            onReminderDeleteClicked = { reminderEntity -> deleteReminderPermanently(reminderEntity) }
        )
        recyclerViewHistory.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(this@HistoryActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        // Set up bottom navigation selections and actions
        findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
            .setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        startActivity(android.content.Intent(this, MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        true
                    }
                    R.id.nav_today -> {
                        // Already on History; do nothing
                        true
                    }
                    else -> false
                }
            }
        // Mark History as selected if menu present
        findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
            .selectedItemId = R.id.nav_today
    }

    private fun observeCompletedReminders() {
        lifecycleScope.launch {
            reminderRepository.completedReminders.collectLatest { completedList ->
                if (completedList.isEmpty()) {
                    recyclerViewHistory.visibility = View.GONE
                    Toast.makeText(
                        this@HistoryActivity,
                        "No history available",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    recyclerViewHistory.visibility = View.VISIBLE
                    historyAdapter.submitList(completedList)
                }
            }
        }
    }

    private fun restoreReminderToActiveList(reminderEntity: ReminderEntity) {
        lifecycleScope.launch {
            val updatedReminder = reminderEntity.copy(
                isCompleted = false,
                status = ReminderStatus.PENDING
            )
            reminderRepository.update(updatedReminder)
            Toast.makeText(
                this@HistoryActivity,
                "'${reminderEntity.title}' restored to active list.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun deleteReminderPermanently(reminderEntity: ReminderEntity) {
        lifecycleScope.launch {
            reminderRepository.delete(reminderEntity)
            Toast.makeText(this@HistoryActivity, "Deleted '${reminderEntity.title}'", Toast.LENGTH_SHORT).show()
        }
    }
}

package com.example.reminderapp.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reminderapp.CreateReminderActivity
import com.example.reminderapp.R
import com.example.reminderapp.ReminderAdapter
import com.example.reminderapp.data.ReminderDatabase
import com.example.reminderapp.data.ReminderRepository
import com.example.reminderapp.ReminderStatus
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private lateinit var recyclerViewReminders: RecyclerView
    private lateinit var fabAddReminder: FloatingActionButton
    private lateinit var reminderAdapter: ReminderAdapter
    private lateinit var reminderRepository: ReminderRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize database
        val database = ReminderDatabase.getDatabase(requireContext())
        reminderRepository = ReminderRepository(database.reminderDao())

        initializeViews(view)
        setupRecyclerView()
        setupClickListeners()
        observeReminders()
    }

    private fun initializeViews(view: View) {
        recyclerViewReminders = view.findViewById(R.id.recyclerViewReminders)
        fabAddReminder = view.findViewById(R.id.fabAddReminder)
        Log.d("HomeFragment", "Views initialized")
    }

    private fun setupRecyclerView() {
        Log.d("HomeFragment", "Setting up RecyclerView")

        reminderAdapter = ReminderAdapter(
            emptyList(),
            onReminderDeleted = { reminderEntity ->
                lifecycleScope.launch {
                    reminderRepository.delete(reminderEntity)
                    android.widget.Toast.makeText(requireContext(), "Reminder deleted: ${reminderEntity.title}", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onReminderMarkedDone = { reminderEntity ->
                lifecycleScope.launch {
                    val updated = reminderEntity.copy(
                        isCompleted = true,
                        status = ReminderStatus.COMPLETED
                    )
                    reminderRepository.update(updated)
                    android.widget.Toast.makeText(requireContext(), "Marked done: ${reminderEntity.title}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
        recyclerViewReminders.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewReminders.adapter = reminderAdapter

        Log.d("HomeFragment", "RecyclerView setup complete")
    }

    private fun observeReminders() {
        lifecycleScope.launch {
            reminderRepository.activeReminders.collectLatest { reminders ->
                Log.d("HomeFragment", "Observed ${reminders.size} active reminders from database")
                reminderAdapter.updateReminders(reminders)
            }
        }
    }

    private fun setupClickListeners() {
        fabAddReminder.setOnClickListener {
            Log.d("HomeFragment", "FAB clicked, launching CreateReminderActivity")
            val intent = Intent(requireContext(), CreateReminderActivity::class.java)
            startActivity(intent)
        }
    }
}

package com.example.reminderapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reminderapp.HistoryAdapter
import com.example.reminderapp.R
import com.example.reminderapp.ReminderStatus
import com.example.reminderapp.data.ReminderDatabase
import com.example.reminderapp.data.ReminderRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {
    private lateinit var recyclerViewHistory: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var reminderRepository: ReminderRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize database
        val database = ReminderDatabase.getDatabase(requireContext())
        reminderRepository = ReminderRepository(database.reminderDao())

        initializeViews(view)
        setupRecyclerView()
        observeCompletedReminders()
    }

    private fun initializeViews(view: View) {
        recyclerViewHistory = view.findViewById(R.id.recyclerViewHistory)
        Log.d("HistoryFragment", "Views initialized")
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            onReminderRestoreClicked = { reminderEntity -> restoreReminderToActiveList(reminderEntity) },
            onReminderDeleteClicked = { reminderEntity -> deleteReminderPermanently(reminderEntity) }
        )
        recyclerViewHistory.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeCompletedReminders() {
        lifecycleScope.launch {
            reminderRepository.completedReminders.collectLatest { completedList ->
                if (completedList.isEmpty()) {
                    recyclerViewHistory.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
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

    private fun restoreReminderToActiveList(reminderEntity: com.example.reminderapp.data.ReminderEntity) {
        lifecycleScope.launch {
            val updatedReminder = reminderEntity.copy(
                isCompleted = false,
                status = ReminderStatus.PENDING
            )
            reminderRepository.update(updatedReminder)
            Toast.makeText(
                requireContext(),
                "'${reminderEntity.title}' restored to active list.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun deleteReminderPermanently(reminderEntity: com.example.reminderapp.data.ReminderEntity) {
        lifecycleScope.launch {
            reminderRepository.delete(reminderEntity)
            Toast.makeText(requireContext(), "Deleted '${reminderEntity.title}'", Toast.LENGTH_SHORT).show()
        }
    }
}

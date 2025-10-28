package com.example.reminderapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.reminderapp.R
import com.example.reminderapp.data.ReminderDatabase
import com.example.reminderapp.data.ReminderRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StatsFragment : Fragment() {
    private lateinit var textViewTotalReminders: TextView
    private lateinit var textViewCompletedReminders: TextView
    private lateinit var textViewPendingReminders: TextView
    private lateinit var textViewCompletionRate: TextView
    private lateinit var reminderRepository: ReminderRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize database
        val database = ReminderDatabase.getDatabase(requireContext())
        reminderRepository = ReminderRepository(database.reminderDao())

        initializeViews(view)
        observeStats()
    }

    private fun initializeViews(view: View) {
        textViewTotalReminders = view.findViewById(R.id.textViewTotalReminders)
        textViewCompletedReminders = view.findViewById(R.id.textViewCompletedReminders)
        textViewPendingReminders = view.findViewById(R.id.textViewPendingReminders)
        textViewCompletionRate = view.findViewById(R.id.textViewCompletionRate)
        Log.d("StatsFragment", "Views initialized")
    }

    private fun observeStats() {
        lifecycleScope.launch {
            // Observe all reminders
            reminderRepository.allReminders.collectLatest { allRemindersList ->
                val completed = allRemindersList.count { it.isCompleted }
                val pending = allRemindersList.count { !it.isCompleted }
                val total = allRemindersList.size
                val completionRate = if (total > 0) (completed * 100.0 / total).toInt() else 0

                textViewTotalReminders.text = total.toString()
                textViewCompletedReminders.text = completed.toString()
                textViewPendingReminders.text = pending.toString()
                textViewCompletionRate.text = "$completionRate%"
            }
        }
    }
}

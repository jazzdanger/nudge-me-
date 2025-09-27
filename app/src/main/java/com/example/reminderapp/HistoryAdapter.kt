package com.example.reminderapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.reminderapp.data.ReminderEntity

class HistoryAdapter(
    private val onReminderRestoreClicked: (ReminderEntity) -> Unit
) : ListAdapter<ReminderEntity, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val reminderEntity = getItem(position)
        holder.bind(reminderEntity, onReminderRestoreClicked)
    }

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.textTitle)
        private val dateTimeTextView: TextView = itemView.findViewById(R.id.textDateTime)

        fun bind(reminder: ReminderEntity, onReminderRestoreClicked: (ReminderEntity) -> Unit) {
            titleTextView.text = reminder.title
            dateTimeTextView.text = "Completed: ${reminder.dateTime}"

            itemView.setOnClickListener {
                onReminderRestoreClicked(reminder)
            }
        }
    }
}

class HistoryDiffCallback : DiffUtil.ItemCallback<ReminderEntity>() {
    override fun areItemsTheSame(oldItem: ReminderEntity, newItem: ReminderEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ReminderEntity, newItem: ReminderEntity): Boolean {
        return oldItem == newItem
    }
}

package com.example.reminderapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.reminderapp.R
import com.example.reminderapp.models.CalendarItem
import java.text.SimpleDateFormat
import java.util.*

class CalendarEventsAdapter : RecyclerView.Adapter<CalendarEventsAdapter.EventViewHolder>() {
    private var items: List<CalendarItem> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun updateEvents(newItems: List<CalendarItem>) {
        items = newItems.sortedWith(Comparator { a, b ->
            val av = a.startMillis ?: Long.MAX_VALUE
            val bv = b.startMillis ?: Long.MAX_VALUE
            av.compareTo(bv)
        })
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewTitle: TextView = itemView.findViewById(R.id.textViewEventTitle)
        private val textViewDate: TextView = itemView.findViewById(R.id.textViewEventDate)
        private val textViewTime: TextView = itemView.findViewById(R.id.textViewEventTime)
        private val textViewDescription: TextView = itemView.findViewById(R.id.textViewEventDescription)
        private val iconContainer: View = itemView.findViewById(R.id.iconContainer)
        private val iconType: android.widget.ImageView = itemView.findViewById(R.id.iconType)
        private val textViewType: TextView = itemView.findViewById(R.id.textViewType)

        fun bind(item: CalendarItem) {
            textViewTitle.text = item.title

            val startMs = item.startMillis
            if (startMs != null) {
                val startDate = Date(startMs)
                textViewDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(startDate)
                textViewTime.text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(startDate)
            } else {
                textViewDate.text = "No date"
                textViewTime.text = if (item.isAllDay) "All day" else ""
            }

            textViewDescription.text = item.description ?: ""

            // Set type-specific UI
            when (item.type) {
                CalendarItem.ItemType.TASK -> {
                    iconContainer.setBackgroundResource(R.drawable.circle_task)
                    iconType.setImageResource(R.drawable.ic_calendar)  // TODO: Add task icon
                    textViewType.apply {
                        text = "Task"
                        visibility = View.VISIBLE
                    }
                }
                CalendarItem.ItemType.BIRTHDAY -> {
                    iconContainer.setBackgroundResource(R.drawable.circle_birthday)
                    iconType.setImageResource(R.drawable.ic_calendar)  // TODO: Add cake/gift icon
                    textViewType.apply {
                        text = "Birthday"
                        visibility = View.VISIBLE
                    }
                }
                else -> {
                    iconContainer.setBackgroundResource(R.drawable.circle_blue)
                    iconType.setImageResource(R.drawable.ic_calendar)
                    textViewType.visibility = View.GONE
                }
            }
        }
    }

    // no-op
}

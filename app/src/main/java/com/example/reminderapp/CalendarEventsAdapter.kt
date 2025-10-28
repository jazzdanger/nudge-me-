package com.example.reminderapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.api.services.calendar.model.Event
import java.text.SimpleDateFormat
import java.util.*

class CalendarEventsAdapter : RecyclerView.Adapter<CalendarEventsAdapter.EventViewHolder>() {
    private var events: List<Event> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun updateEvents(newEvents: List<Event>) {
        events = newEvents
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewTitle: TextView = itemView.findViewById(R.id.textViewEventTitle)
        private val textViewDate: TextView = itemView.findViewById(R.id.textViewEventDate)
        private val textViewTime: TextView = itemView.findViewById(R.id.textViewEventTime)
        private val textViewDescription: TextView = itemView.findViewById(R.id.textViewEventDescription)

        fun bind(event: Event) {
            textViewTitle.text = event.summary ?: "No Title"
            
            val start = event.start
            val end = event.end
            
            when {
                start?.dateTime != null -> {
                    // All-day event
                    val startDate = Date(start.dateTime.value)
                    textViewDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(startDate)
                    textViewTime.text = "All day"
                }
                start?.date != null -> {
                    // Date-only event
                    val startDate = Date(start.date.value)
                    textViewDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(startDate)
                    textViewTime.text = "All day"
                }
                else -> {
                    textViewDate.text = "No date"
                    textViewTime.text = ""
                }
            }
            
            textViewDescription.text = event.description ?: ""
        }
    }
}

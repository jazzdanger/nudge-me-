package com.example.reminderapp

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.reminderapp.data.ReminderEntity

class ReminderAdapter(
    private var reminders: List<ReminderEntity>,
    private val onReminderDeleted: ((ReminderEntity) -> Unit)? = null,
    private val onReminderMarkedDone: ((ReminderEntity) -> Unit)? = null
) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    companion object {
        private const val TAG = "ReminderAdapter"
    }

    class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconContainer: FrameLayout = itemView.findViewById(R.id.iconContainer)
        val iconReminder: ImageView = itemView.findViewById(R.id.iconReminder)
        val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        val textDateTime: TextView = itemView.findViewById(R.id.textDateTime)
        val iconStatus: ImageView = itemView.findViewById(R.id.iconStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder, parent, false)
        Log.d(TAG, "Created ViewHolder")
        return ReminderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = reminders[position]
        Log.d(TAG, "Binding reminder at position $position: ${reminder.title}")

        // Set icon and background
        setReminderIcon(holder, reminder)

        holder.textTitle.text = reminder.title
        holder.textDateTime.text = reminder.dateTime

    // Let the CardView defined in the layout control its background and elevation
    // (Removing a forced transparent background so the card's lifted grey surface shows)

        // Status icon
        when (reminder.status) {
            ReminderStatus.PENDING -> {
                holder.iconStatus.setImageResource(R.drawable.ic_circle_empty)
                holder.iconStatus.setColorFilter(holder.itemView.context.getColor(R.color.blue_500))
            }

            ReminderStatus.ACTIVE -> {
                holder.iconStatus.setImageResource(R.drawable.ic_check_circle)
                holder.itemView.setBackgroundResource(R.color.light_green)
            }

            ReminderStatus.COMPLETED -> {
                holder.iconStatus.setImageResource(R.drawable.ic_check_circle)
            }
        }



        holder.itemView.setOnClickListener {
            Log.d(TAG, "Reminder item clicked: ${reminder.title}")
        }

        holder.iconStatus.setOnClickListener {
            if (!reminder.isCompleted) {
                Log.d(TAG, "Reminder marked done clicked: ${reminder.title}")
                onReminderMarkedDone?.invoke(reminder)
            }
        }
    }

    private fun setReminderIcon(holder: ReminderViewHolder, reminder: ReminderEntity) {
        val backgroundRes = when (reminder.iconResId) {
            R.drawable.ic_calendar -> R.drawable.circle_blue
            R.drawable.ic_bell -> R.drawable.circle_green
            R.drawable.ic_shopping_cart -> R.drawable.circle_red
            R.drawable.ic_bag -> R.drawable.circle_orange
            else -> R.drawable.circle_blue
        }

        holder.iconContainer.setBackgroundResource(backgroundRes)
        holder.iconReminder.setImageResource(reminder.iconResId)
    }

    override fun getItemCount(): Int = reminders.size

    fun updateReminders(newReminders: List<ReminderEntity>) {
        Log.d(TAG, "updateReminders called with ${newReminders.size} reminders")
        reminders = newReminders
        notifyDataSetChanged()
    }
}
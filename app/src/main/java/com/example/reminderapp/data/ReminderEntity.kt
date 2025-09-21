package com.example.reminderapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.reminderapp.Reminder
import com.example.reminderapp.ReminderStatus

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val dateTime: String,
    val iconResId: Int,
    val status: ReminderStatus,
    val repeatDays: String = "" // Store as comma-separated string
) {
    fun toReminder(): Reminder {
        val days = if (repeatDays.isEmpty()) {
            emptySet()
        } else {
            repeatDays.split(",").map { it.toInt() }.toSet()
        }
        return Reminder(id, title, dateTime, iconResId, status, days)
    }
    
    companion object {
        fun fromReminder(reminder: Reminder): ReminderEntity {
            val daysString = reminder.repeatDays.joinToString(",")
            return ReminderEntity(
                id = reminder.id,
                title = reminder.title,
                dateTime = reminder.dateTime,
                iconResId = reminder.iconResId,
                status = reminder.status,
                repeatDays = daysString
            )
        }
    }
}

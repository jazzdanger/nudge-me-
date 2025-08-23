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
    val status: ReminderStatus
) {
    fun toReminder(): Reminder {
        return Reminder(id, title, dateTime, iconResId, status)
    }
    
    companion object {
        fun fromReminder(reminder: Reminder): ReminderEntity {
            return ReminderEntity(
                id = reminder.id,
                title = reminder.title,
                dateTime = reminder.dateTime,
                iconResId = reminder.iconResId,
                status = reminder.status
            )
        }
    }
}

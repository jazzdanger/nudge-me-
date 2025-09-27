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
    val repeatDays: String = "", // Store as comma-separated string
    val isCompleted: Boolean = false,
    val notes: String? = null,
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val locationTriggerType: String? = null
) {

    fun toReminder(): Reminder {
        val days = if (repeatDays.isEmpty()) emptySet() else repeatDays.split(",").map { it.toInt() }.toSet()
        return Reminder(
            id = id,
            title = title,
            dateTime = dateTime,
            iconResId = iconResId,
            status = status,
            repeatDays = days,
            isCompleted = isCompleted,
            notes = notes,
            locationLatitude = locationLatitude,
            locationLongitude = locationLongitude,
            locationTriggerType = locationTriggerType
        )
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
                repeatDays = daysString,
                isCompleted = reminder.isCompleted,
                notes = reminder.notes,
                locationLatitude = reminder.locationLatitude,
                locationLongitude = reminder.locationLongitude,
                locationTriggerType = reminder.locationTriggerType
            )
        }
    }
}

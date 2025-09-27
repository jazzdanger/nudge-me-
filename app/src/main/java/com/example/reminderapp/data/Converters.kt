package com.example.reminderapp.data

import androidx.room.TypeConverter
import com.example.reminderapp.ReminderStatus

class Converters {

    // ---------------------------
    // ReminderStatus <-> String
    // ---------------------------
    @TypeConverter
    fun fromReminderStatus(status: ReminderStatus): String {
        return status.name
    }

    @TypeConverter
    fun toReminderStatus(value: String): ReminderStatus {
        return ReminderStatus.valueOf(value)
    }

    // ---------------------------
    // Set<Int> <-> String
    // ---------------------------
    @TypeConverter
    fun fromRepeatDays(days: Set<Int>): String {
        return days.joinToString(",")
    }

    @TypeConverter
    fun toRepeatDays(value: String): Set<Int> {
        return if (value.isEmpty()) emptySet() else value.split(",").map { it.toInt() }.toSet()
    }
}

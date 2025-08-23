package com.example.reminderapp.data

import androidx.room.TypeConverter
import com.example.reminderapp.ReminderStatus

class Converters {
    @TypeConverter
    fun fromReminderStatus(status: ReminderStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toReminderStatus(status: String): ReminderStatus {
        return ReminderStatus.valueOf(status)
    }
}

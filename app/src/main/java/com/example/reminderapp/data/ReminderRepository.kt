package com.example.reminderapp.data

import com.example.reminderapp.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReminderRepository(private val reminderDao: ReminderDao) {
    
    val allReminders: Flow<List<Reminder>> = reminderDao.getAllReminders().map { entities ->
        entities.map { it.toReminder() }
    }
    
    suspend fun insertReminder(reminder: Reminder): Long {
        return reminderDao.insertReminder(ReminderEntity.fromReminder(reminder))
    }
    
    suspend fun updateReminder(reminder: Reminder) {
        reminderDao.updateReminder(ReminderEntity.fromReminder(reminder))
    }
    
    suspend fun deleteReminder(reminder: Reminder) {
        reminderDao.deleteReminder(ReminderEntity.fromReminder(reminder))
    }
    
    suspend fun deleteReminderById(reminderId: Long) {
        reminderDao.deleteReminderById(reminderId)
    }
}

package com.example.reminderapp.data

import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val reminderDao: ReminderDao) {

    // Active reminders (isCompleted = 0)
    val activeReminders: Flow<List<ReminderEntity>> = reminderDao.getActiveReminders()

    // Completed reminders (History)
    val completedReminders: Flow<List<ReminderEntity>> = reminderDao.getCompletedReminders()

    // All reminders (for stats)
    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()

    // Insert a new reminder
    suspend fun insert(reminder: ReminderEntity) {
        reminderDao.insertReminder(reminder)
    }

    // Update a reminder (e.g., mark as completed / restore)
    suspend fun update(reminderEntity: ReminderEntity) {
        reminderDao.updateReminder(reminderEntity)
    }

    // Delete a reminder
    suspend fun delete(reminder: ReminderEntity) {
        reminderDao.deleteReminder(reminder)
    }

    // Delete a reminder by ID
    suspend fun deleteById(id: Long) {
        reminderDao.getReminderById(id)?.let { reminderDao.deleteReminder(it) }
    }

    // Get a single reminder by ID
    suspend fun getReminderById(id: Long): ReminderEntity? {
        return reminderDao.getReminderById(id)
    }
}

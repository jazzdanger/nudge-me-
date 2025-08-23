package com.example.reminderapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY id DESC")
    fun getAllReminders(): Flow<List<ReminderEntity>>
    
    @Insert
    suspend fun insertReminder(reminder: ReminderEntity): Long
    
    @Update
    suspend fun updateReminder(reminder: ReminderEntity)
    
    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)
    
    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: Long)
    
    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    suspend fun getReminderById(reminderId: Long): ReminderEntity?
}

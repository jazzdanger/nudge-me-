package com.example.reminderapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY id DESC") // Or order by dateTime
    fun getActiveReminders(): Flow<List<ReminderEntity>> // For Home Page

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY id DESC") // Or order by dateTime
    fun getCompletedReminders(): Flow<List<ReminderEntity>> // For History Page

    @Query("SELECT * FROM reminders ORDER BY id DESC") // All reminders for stats
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity) // This will be used to move to/from history

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity) // For permanent deletion (optional from history)

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?
}

package com.example.reminderapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.reminderapp.data.ReminderEntity

@Database(
    entities = [ReminderEntity::class],
    version = 3, // Incremented to match schema changes
    exportSchema = true // Recommended to track database schema
)
@TypeConverters(Converters::class) // Needed for ReminderStatus or other custom types
abstract class ReminderDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        fun getDatabase(context: Context): ReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "reminder_database"
                )
                    .fallbackToDestructiveMigration() // wipes old DB, creates new one
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

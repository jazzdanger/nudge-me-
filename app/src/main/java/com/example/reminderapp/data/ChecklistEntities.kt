package com.example.reminderapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checklist_lists")
data class ChecklistListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "checklist_items")
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val title: String,
    val isChecked: Boolean = false
)



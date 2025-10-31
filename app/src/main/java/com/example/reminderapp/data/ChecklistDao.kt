package com.example.reminderapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist_lists LIMIT 1")
    fun getDefaultList(): Flow<ChecklistListEntity?>

    @Query("SELECT * FROM checklist_lists ORDER BY id DESC")
    fun getAllLists(): Flow<List<ChecklistListEntity>>

    @Query("SELECT * FROM checklist_lists WHERE id = :id LIMIT 1")
    fun getListById(id: Long): Flow<ChecklistListEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ChecklistListEntity): Long

    @Update
    suspend fun updateList(list: ChecklistListEntity)

    // Items
    @Query("SELECT * FROM checklist_items WHERE listId = :listId ORDER BY id DESC")
    fun getItems(listId: Long): Flow<List<ChecklistItemEntity>>

    @Query("SELECT COUNT(*) FROM checklist_items WHERE listId = :listId")
    fun getItemsCount(listId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ChecklistItemEntity)

    @Update
    suspend fun updateItem(item: ChecklistItemEntity)

    @Delete
    suspend fun deleteItem(item: ChecklistItemEntity)
}



package com.example.reminderapp.data

import kotlinx.coroutines.flow.Flow

class ChecklistRepository(private val checklistDao: ChecklistDao) {
    fun getOrCreateDefaultList(): Flow<ChecklistListEntity?> = checklistDao.getDefaultList()
    fun getAllLists(): Flow<List<ChecklistListEntity>> = checklistDao.getAllLists()
    fun getListById(id: Long): Flow<ChecklistListEntity?> = checklistDao.getListById(id)

    suspend fun ensureDefaultListExists(): Long {
        // Try create a default list if none exists
        return checklistDao.insertList(ChecklistListEntity(name = "Reminders"))
    }

    fun getItems(listId: Long): Flow<List<ChecklistItemEntity>> = checklistDao.getItems(listId)
    fun getItemsCount(listId: Long): Flow<Int> = checklistDao.getItemsCount(listId)

    suspend fun addItem(listId: Long, title: String) {
        checklistDao.insertItem(ChecklistItemEntity(listId = listId, title = title))
    }

    suspend fun toggleItem(item: ChecklistItemEntity) {
        checklistDao.updateItem(item.copy(isChecked = !item.isChecked))
    }

    suspend fun deleteItem(item: ChecklistItemEntity) {
        checklistDao.deleteItem(item)
    }

    suspend fun renameList(list: ChecklistListEntity, newName: String) {
        checklistDao.updateList(list.copy(name = newName))
    }

    suspend fun createList(name: String): Long {
        return checklistDao.insertList(ChecklistListEntity(name = name))
    }
}



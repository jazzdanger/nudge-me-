package com.example.reminderapp.models

data class CalendarItem(
    val id: String,
    val title: String,
    val startMillis: Long?,
    val isAllDay: Boolean = false,
    val description: String? = null,
    val type: ItemType = ItemType.EVENT
) {
    enum class ItemType { EVENT, BIRTHDAY, TASK }
}

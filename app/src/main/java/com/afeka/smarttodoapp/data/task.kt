package com.afeka.smarttodoapp.data

data class Task(
    val id: String = "",
    val userId: String = "",  // Changed from Int to String (Firebase UID)
    val title: String = "",
    val description: String = "",
    val dueDate: Long? = null,
    val priority: String = "MEDIUM",  // Changed from enum to String for Firestore
    val category: String = "General",
    val isCompleted: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class Priority {
    HIGH,
    MEDIUM,
    LOW
}
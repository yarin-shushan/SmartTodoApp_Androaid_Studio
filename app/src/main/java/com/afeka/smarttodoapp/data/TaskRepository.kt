package com.afeka.smarttodoapp.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class TaskRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    // Get all tasks for current user
    fun getAllTasks(): LiveData<List<Task>> {
        val liveData = MutableLiveData<List<Task>>()
        val userId = getCurrentUserId()

        firestore.collection("tasks")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isArchived", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    liveData.value = emptyList()
                    return@addSnapshotListener
                }

                val tasks = snapshot?.toObjects(Task::class.java) ?: emptyList()
                liveData.value = tasks
            }

        return liveData
    }

    // Get incomplete tasks
    fun getIncompleteTasks(): LiveData<List<Task>> {
        val liveData = MutableLiveData<List<Task>>()
        val userId = getCurrentUserId()

        firestore.collection("tasks")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isCompleted", false)
            .whereEqualTo("isArchived", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    liveData.value = emptyList()
                    return@addSnapshotListener
                }

                val tasks = snapshot?.toObjects(Task::class.java) ?: emptyList()
                liveData.value = tasks
            }

        return liveData
    }

    // Get completed tasks
    fun getCompletedTasks(): LiveData<List<Task>> {
        val liveData = MutableLiveData<List<Task>>()
        val userId = getCurrentUserId()

        firestore.collection("tasks")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isCompleted", true)
            .whereEqualTo("isArchived", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    liveData.value = emptyList()
                    return@addSnapshotListener
                }

                val tasks = snapshot?.toObjects(Task::class.java) ?: emptyList()
                liveData.value = tasks
            }

        return liveData
    }

    // Get archived tasks
    fun getArchivedTasks(): LiveData<List<Task>> {
        val liveData = MutableLiveData<List<Task>>()
        val userId = getCurrentUserId()

        firestore.collection("tasks")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isArchived", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    liveData.value = emptyList()
                    return@addSnapshotListener
                }

                val tasks = snapshot?.toObjects(Task::class.java) ?: emptyList()
                liveData.value = tasks
            }

        return liveData
    }

    // Insert task
    suspend fun insert(task: Task) {
        try {
            val userId = getCurrentUserId()
            val taskWithId = if (task.id.isEmpty()) {
                val docRef = firestore.collection("tasks").document()
                task.copy(id = docRef.id, userId = userId)
            } else {
                task.copy(userId = userId)
            }

            firestore.collection("tasks")
                .document(taskWithId.id)
                .set(taskWithId)
                .await()
        } catch (e: Exception) {
            // Handle error
            throw e
        }
    }

    // Update task
    suspend fun update(task: Task) {
        try {
            firestore.collection("tasks")
                .document(task.id)
                .set(task)
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    // Delete task
    suspend fun delete(task: Task) {
        try {
            firestore.collection("tasks")
                .document(task.id)
                .delete()
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    // Archive task
    suspend fun archiveTask(taskId: String) {
        try {
            firestore.collection("tasks")
                .document(taskId)
                .update("isArchived", true)
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    // Unarchive task
    suspend fun unarchiveTask(taskId: String) {
        try {
            firestore.collection("tasks")
                .document(taskId)
                .update("isArchived", false)
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    // Get task by ID
    suspend fun getTaskById(taskId: String): Task? {
        return try {
            val document = firestore.collection("tasks")
                .document(taskId)
                .get()
                .await()

            document.toObject(Task::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Get tasks by category
    fun getTasksByCategory(category: String): LiveData<List<Task>> {
        val liveData = MutableLiveData<List<Task>>()
        val userId = getCurrentUserId()

        firestore.collection("tasks")
            .whereEqualTo("userId", userId)
            .whereEqualTo("category", category)
            .whereEqualTo("isArchived", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    liveData.value = emptyList()
                    return@addSnapshotListener
                }

                val tasks = snapshot?.toObjects(Task::class.java) ?: emptyList()
                liveData.value = tasks
            }

        return liveData
    }

    // Delete all tasks
    suspend fun deleteAllTasks() {
        try {
            val userId = getCurrentUserId()
            val snapshot = firestore.collection("tasks")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val batch = firestore.batch()
            for (document in snapshot.documents) {
                batch.delete(document.reference)
            }
            batch.commit().await()
        } catch (e: Exception) {
            throw e
        }
    }
}
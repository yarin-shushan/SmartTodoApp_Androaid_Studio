package com.afeka.smarttodoapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.afeka.smarttodoapp.data.Task
import com.afeka.smarttodoapp.data.TaskRepository
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository = TaskRepository()

    // Current sort type
    private val _sortType = MutableLiveData<SortType>(SortType.CREATED_DATE)

    // All tasks - LiveData with sorting
    val allTasks: LiveData<List<Task>> = repository.getAllTasks().map { tasks ->
        sortTasks(tasks, _sortType.value ?: SortType.CREATED_DATE)
    }

    val incompleteTasks: LiveData<List<Task>> = repository.getIncompleteTasks()

    val completedTasks: LiveData<List<Task>> = repository.getCompletedTasks()

    // Archived tasks
    val archivedTasks: LiveData<List<Task>> = repository.getArchivedTasks()

    // Set user ID (no longer needed - Firebase Auth handles it)
    fun setUserId(userId: Int) {
        // No-op: userId is now handled by Firebase Auth in repository
    }

    // Set sort type
    fun setSortType(sortType: SortType) {
        _sortType.value = sortType
    }

    // Sort tasks based on type
    private fun sortTasks(tasks: List<Task>, sortType: SortType): List<Task> {
        return when (sortType) {
            SortType.DUE_DATE -> tasks.sortedWith(compareBy(nullsLast()) { it.dueDate })
            SortType.PRIORITY -> tasks.sortedBy {
                when (it.priority) {
                    "HIGH" -> 0
                    "MEDIUM" -> 1
                    "LOW" -> 2
                    else -> 3
                }
            }
            SortType.CREATED_DATE -> tasks.sortedByDescending { it.createdAt }
        }
    }

    // Add task
    fun insert(task: Task) = viewModelScope.launch {
        repository.insert(task)
    }

    // Update task
    fun update(task: Task) = viewModelScope.launch {
        repository.update(task)
    }

    // Delete task
    fun delete(task: Task) = viewModelScope.launch {
        repository.delete(task)
    }

    // Toggle task completion
    fun toggleTaskCompletion(task: Task) = viewModelScope.launch {
        val updatedTask = task.copy(isCompleted = !task.isCompleted)
        repository.update(updatedTask)
    }

    // Archive task
    fun archiveTask(taskId: String) = viewModelScope.launch {
        repository.archiveTask(taskId)
    }

    // Unarchive task (restore from archive)
    fun unarchiveTask(taskId: String) = viewModelScope.launch {
        repository.unarchiveTask(taskId)
    }

    // Get tasks by category
    fun getTasksByCategory(category: String): LiveData<List<Task>> {
        return repository.getTasksByCategory(category).map { tasks ->
            sortTasks(tasks, _sortType.value ?: SortType.CREATED_DATE)
        }
    }

    // Delete all tasks
    fun deleteAllTasks() = viewModelScope.launch {
        repository.deleteAllTasks()
    }

    // Sort types enum
    enum class SortType {
        DUE_DATE,
        PRIORITY,
        CREATED_DATE
    }
}
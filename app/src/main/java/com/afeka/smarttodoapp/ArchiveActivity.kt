package com.afeka.smarttodoapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afeka.smarttodoapp.adapter.TaskAdapter
import com.afeka.smarttodoapp.data.Task
import com.afeka.smarttodoapp.viewmodel.TaskViewModel
import com.google.android.material.card.MaterialCardView

class ArchiveActivity : AppCompatActivity() {

    private lateinit var taskViewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var cardEmptyArchive: MaterialCardView
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide status bar
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        setContentView(R.layout.activity_archive)

        // Setup toolbar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Get user ID
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        userId = prefs.getInt("user_id", -1)

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewArchive)
        cardEmptyArchive = findViewById(R.id.cardEmptyArchive)

        // Initialize ViewModel
        taskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]
        taskViewModel.setUserId(userId)

        // Setup RecyclerView
        setupRecyclerView()

        // Observe archived tasks
        observeArchivedTasks()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClick = { task ->
                // Show task details
                showTaskDetails(task)
            },
            onTaskLongClick = { task ->
                // Option to restore from archive
                showRestoreDialog(task)
            },
            onDeleteClick = { task ->
                // Delete permanently
                deleteTaskPermanently(task)
            },
            onCheckboxClick = { task ->
                // Restore from archive when unchecking
                showRestoreDialog(task)
            }
        )

        recyclerView.apply {
            adapter = taskAdapter
            layoutManager = LinearLayoutManager(this@ArchiveActivity)
        }
    }

    private fun observeArchivedTasks() {
        taskViewModel.archivedTasks.observe(this) { tasks ->
            if (tasks.isNullOrEmpty()) {
                // Show empty state card
                recyclerView.visibility = View.GONE
                cardEmptyArchive.visibility = View.VISIBLE
            } else {
                // Show tasks
                recyclerView.visibility = View.VISIBLE
                cardEmptyArchive.visibility = View.GONE
                taskAdapter.submitList(tasks)
            }
        }
    }

    private fun showTaskDetails(task: Task) {
        AlertDialog.Builder(this)
            .setTitle(task.title)
            .setMessage(
                "Description: ${task.description}\n\n" +
                        "Category: ${task.category}\n" +
                        "Priority: ${task.priority}\n" +
                        "Status: Completed & Archived"
            )
            .setPositiveButton("Restore") { _, _ ->
                restoreTask(task)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showRestoreDialog(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Restore Task?")
            .setMessage("Would you like to restore '${task.title}' from archive?")
            .setPositiveButton("Restore") { _, _ ->
                restoreTask(task)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun restoreTask(task: Task) {
        taskViewModel.unarchiveTask(task.id)
        Toast.makeText(this, "Task restored", Toast.LENGTH_SHORT).show()
    }

    private fun deleteTaskPermanently(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Delete Permanently?")
            .setMessage("Are you sure you want to permanently delete '${task.title}'? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                taskViewModel.delete(task)
                Toast.makeText(this, "Task deleted permanently", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
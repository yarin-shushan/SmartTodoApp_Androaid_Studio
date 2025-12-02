package com.afeka.smarttodoapp

import android.os.Bundle
import android.view.View
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afeka.smarttodoapp.adapter.TaskAdapter
import com.afeka.smarttodoapp.data.Task
import com.afeka.smarttodoapp.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var taskViewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var calendarView: CalendarView
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvTaskCount: TextView
    private lateinit var tvEmptyState: TextView
    private var userId: Int = -1
    private var selectedDateMillis: Long = System.currentTimeMillis()
    private val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide status bar
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        setContentView(R.layout.activity_calendar)

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
        calendarView = findViewById(R.id.calendarView)
        recyclerView = findViewById(R.id.recyclerViewTasks)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        tvTaskCount = findViewById(R.id.tvTaskCount)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        // Initialize ViewModel
        taskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]
        taskViewModel.setUserId(userId)

        // Setup RecyclerView
        setupRecyclerView()

        // Setup Calendar
        setupCalendar()

        // Load tasks for today
        loadTasksForDate(selectedDateMillis)
    }

    private fun setupCalendar() {
        // Set calendar listener
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // Create calendar instance for selected date
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            selectedDateMillis = calendar.timeInMillis
            loadTasksForDate(selectedDateMillis)
        }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClick = { task ->
                // Show task details
                showTaskDetails(task)
            },
            onTaskLongClick = { task ->
                Toast.makeText(this, "Long click: ${task.title}", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { task ->
                // Delete task
                deleteTask(task)
            },
            onCheckboxClick = { task ->
                // Toggle completion
                taskViewModel.toggleTaskCompletion(task)
            }
        )

        recyclerView.apply {
            adapter = taskAdapter
            layoutManager = LinearLayoutManager(this@CalendarActivity)
        }
    }

    private fun loadTasksForDate(dateMillis: Long) {
        // Update selected date text
        val formattedDate = dateFormat.format(Date(dateMillis))
        tvSelectedDate.text = "Tasks for: $formattedDate"

        // Calculate start and end of day
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        // Observe all tasks and filter by date
        taskViewModel.allTasks.observe(this) { allTasks ->
            val tasksForDate = allTasks.filter { task ->
                task.dueDate != null && task.dueDate in startOfDay..endOfDay
            }

            if (tasksForDate.isEmpty()) {
                // Show empty state
                recyclerView.visibility = View.GONE
                tvEmptyState.visibility = View.VISIBLE
                tvTaskCount.text = "0"
            } else {
                // Show tasks
                recyclerView.visibility = View.VISIBLE
                tvEmptyState.visibility = View.GONE
                taskAdapter.submitList(tasksForDate)
                tvTaskCount.text = tasksForDate.size.toString()
            }
        }
    }

    private fun showTaskDetails(task: Task) {
        val dueDateText = if (task.dueDate != null) {
            dateFormat.format(Date(task.dueDate))
        } else {
            "No due date"
        }

        AlertDialog.Builder(this)
            .setTitle(task.title)
            .setMessage(
                "Description: ${task.description}\n\n" +
                        "Category: ${task.category}\n" +
                        "Priority: ${task.priority}\n" +
                        "Due Date: $dueDateText\n" +
                        "Status: ${if (task.isCompleted) "Completed" else "Pending"}"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun deleteTask(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete '${task.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                taskViewModel.delete(task)
                Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
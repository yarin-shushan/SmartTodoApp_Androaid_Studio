package com.afeka.smarttodoapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afeka.smarttodoapp.adapter.TaskAdapter
import com.afeka.smarttodoapp.data.Task
import com.afeka.smarttodoapp.viewmodel.TaskViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var taskViewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var fabStatistics: FloatingActionButton
    private lateinit var spinnerCategoryFilter: Spinner
    private lateinit var auth: FirebaseAuth
    private var allTasksList: List<Task> = emptyList()

    private val categories = arrayOf("All", "General", "Work", "Personal", "Shopping", "Study")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide status bar
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check if user is logged in
        if (auth.currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Setup toolbar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Initialize ViewModel
        taskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]

        recyclerView = findViewById(R.id.recyclerView)
        fabAddTask = findViewById(R.id.fabAddTask)
        fabStatistics = findViewById(R.id.fabStatistics)
        spinnerCategoryFilter = findViewById(R.id.spinnerCategoryFilter)

        setupRecyclerView()

        setupCategoryFilter()

        observeTasks()

        // Add task button
        fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }

        // Statistics button
        fabStatistics.setOnClickListener {
            openStatistics()
        }
    }

    private fun openStatistics() {
        val intent = Intent(this, StatisticsActivity::class.java)
        startActivity(intent)
    }

    private fun openArchive() {
        val intent = Intent(this, ArchiveActivity::class.java)
        startActivity(intent)
    }

    private fun openCalendar() {
        val intent = Intent(this, CalendarActivity::class.java)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // Setup SearchView
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView

        searchView?.queryHint = "Search tasks..."
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterTasks(newText ?: "")
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_calendar -> {
                openCalendar()
                true
            }
            R.id.action_archive -> {
                openArchive()
                true
            }
            R.id.sort_by_date -> {
                taskViewModel.setSortType(TaskViewModel.SortType.DUE_DATE)
                Toast.makeText(this, "Sorted by Due Date", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.sort_by_priority -> {
                taskViewModel.setSortType(TaskViewModel.SortType.PRIORITY)
                Toast.makeText(this, "Sorted by Priority", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.sort_by_created -> {
                taskViewModel.setSortType(TaskViewModel.SortType.CREATED_DATE)
                Toast.makeText(this, "Sorted by Created Date", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun filterTasks(query: String) {
        val filteredList = if (query.isEmpty()) {
            allTasksList
        } else {
            allTasksList.filter { task ->
                task.title.contains(query, ignoreCase = true) ||
                        task.description.contains(query, ignoreCase = true)
            }
        }
        taskAdapter.submitList(filteredList)
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClick = { task ->
                // Click on task - edit
                showEditTaskDialog(task)
            },
            onTaskLongClick = { task ->
                Toast.makeText(this, "Long click: ${task.title}", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { task ->
                // Delete task
                deleteTask(task)
            },
            onCheckboxClick = { task ->
                // When marking as completed, ask to archive
                handleTaskCompletion(task)
            }
        )

        recyclerView.apply {
            adapter = taskAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun handleTaskCompletion(task: Task) {
        // Toggle completion
        val wasCompleted = task.isCompleted
        taskViewModel.toggleTaskCompletion(task)

        // If task was just marked as completed, ask to archive
        if (!wasCompleted) {
            AlertDialog.Builder(this)
                .setTitle("Archive Task?")
                .setMessage("Would you like to move '${task.title}' to archive?")
                .setPositiveButton("Archive") { _, _ ->
                    taskViewModel.archiveTask(task.id)
                    Toast.makeText(this, "Task archived", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Keep in List") { _, _ ->
                    Toast.makeText(this, "Task completed", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
    }

    //setting the filter
    private fun setupCategoryFilter() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoryFilter.adapter = adapter

        spinnerCategoryFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                filterTasksByCategory(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
    }
    //filter mission by category
    private fun filterTasksByCategory(category: String) {
        if (category == "All") {
            taskViewModel.allTasks.observe(this) { tasks ->
                tasks?.let {
                    allTasksList = it
                    taskAdapter.submitList(it)
                }
            }
        } else {
            taskViewModel.getTasksByCategory(category).observe(this) { tasks ->
                tasks?.let {
                    allTasksList = it
                    taskAdapter.submitList(it)
                }
            }
        }
    }
    //watch task
    private fun observeTasks() {
        taskViewModel.allTasks.observe(this) { tasks ->
            tasks?.let {
                allTasksList = it
                taskAdapter.submitList(it)
            }
        }
    }

    private fun showAddTaskDialog() {
        AddEditTaskDialog(
            context = this,
            task = null,
            onSave = { task ->
                taskViewModel.insert(task)
                Toast.makeText(this, "Task added!", Toast.LENGTH_SHORT).show()
            }
        ).show()
    }

    private fun showEditTaskDialog(task: Task) {
        AddEditTaskDialog(
            context = this,
            task = task,
            onSave = { updatedTask ->
                taskViewModel.update(updatedTask)
                Toast.makeText(this, "Task updated!", Toast.LENGTH_SHORT).show()
            }
        ).show()
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
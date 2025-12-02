package com.afeka.smarttodoapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.afeka.smarttodoapp.viewmodel.TaskViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class StatisticsActivity : AppCompatActivity() {

    private lateinit var taskViewModel: TaskViewModel
    private lateinit var pieChartStatus: PieChart
    private lateinit var pieChartCategory: PieChart
    private lateinit var tvTotalTasks: TextView
    private lateinit var tvCompletedTasks: TextView
    private lateinit var tvPendingTasks: TextView
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide status bar
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        setContentView(R.layout.activity_statistics)

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
        pieChartStatus = findViewById(R.id.pieChartStatus)
        pieChartCategory = findViewById(R.id.pieChartCategory)
        tvTotalTasks = findViewById(R.id.tvTotalTasks)
        tvCompletedTasks = findViewById(R.id.tvCompletedTasks)
        tvPendingTasks = findViewById(R.id.tvPendingTasks)

        // Initialize ViewModel
        taskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]
        taskViewModel.setUserId(userId)

        // Load data
        loadStatistics()
    }

    private fun loadStatistics() {
        taskViewModel.allTasks.observe(this) { tasks ->
            if (tasks.isNullOrEmpty()) {
                // No tasks - show empty state
                tvTotalTasks.text = "0"
                tvCompletedTasks.text = "0"
                tvPendingTasks.text = "0"
                setupEmptyChart(pieChartStatus, "No tasks yet")
                setupEmptyChart(pieChartCategory, "No tasks yet")
                return@observe
            }

            // Calculate statistics
            val currentTime = System.currentTimeMillis()
            val totalTasks = tasks.size
            val completedTasks = tasks.count { it.isCompleted }

            // Count overdue tasks (not completed + past due date)
            val overdueTasks = tasks.count { task ->
                !task.isCompleted && task.dueDate != null && task.dueDate < currentTime
            }

            // Pending tasks = not completed - overdue
            val pendingTasks = tasks.count { !it.isCompleted } - overdueTasks

            // Update summary
            tvTotalTasks.text = totalTasks.toString()
            tvCompletedTasks.text = completedTasks.toString()
            tvPendingTasks.text = pendingTasks.toString()

            // Setup pie charts
            setupStatusChart(completedTasks, pendingTasks, overdueTasks)
            setupCategoryChart(tasks)
        }
    }

    private fun setupStatusChart(completed: Int, pending: Int, overdue: Int) {
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        // Add completed (green)
        if (completed > 0) {
            entries.add(PieEntry(completed.toFloat(), "Done"))
            colors.add(Color.parseColor("#4CAF50")) // Green
        }

        // Add pending (orange)
        if (pending > 0) {
            entries.add(PieEntry(pending.toFloat(), "Pend"))
            colors.add(Color.parseColor("#FF9800")) // Orange
        }

        // Add overdue (red)
        if (overdue > 0) {
            entries.add(PieEntry(overdue.toFloat(), "Late"))
            colors.add(Color.parseColor("#F44336")) // Red
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(pieChartStatus))

        pieChartStatus.apply {
            this.data = data
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 40f
            transparentCircleRadius = 45f
            setEntryLabelTextSize(10f)
            setEntryLabelColor(Color.BLACK)
            animateY(1000)

            // Configure legend to prevent overlapping
            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                textSize = 10f
                formSize = 10f
                xEntrySpace = 7f
                yEntrySpace = 5f
                formToTextSpace = 5f
                isWordWrapEnabled = true
            }

            invalidate()
        }
    }

    private fun setupCategoryChart(tasks: List<com.afeka.smarttodoapp.data.Task>) {
        val categoryCount = tasks.groupBy { it.category }
            .mapValues { it.value.size }

        val entries = categoryCount.map { (category, count) ->
            // Shorten category names
            val shortName = when(category) {
                "General" -> "Gen"
                "Personal" -> "Pers"
                "Shopping" -> "Shop"
                else -> category.take(4)
            }
            PieEntry(count.toFloat(), shortName)
        }

        val dataSet = PieDataSet(entries, "")

        // Use different colors for each category
        val colors = mutableListOf<Int>()
        colors.add(ColorTemplate.MATERIAL_COLORS[0])
        colors.add(ColorTemplate.MATERIAL_COLORS[1])
        colors.add(ColorTemplate.MATERIAL_COLORS[2])
        colors.add(ColorTemplate.MATERIAL_COLORS[3])
        colors.add(ColorTemplate.COLORFUL_COLORS[0])
        colors.add(ColorTemplate.COLORFUL_COLORS[1])

        dataSet.colors = colors
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(pieChartCategory))

        pieChartCategory.apply {
            this.data = data
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 40f
            transparentCircleRadius = 45f
            setEntryLabelTextSize(10f)
            setEntryLabelColor(Color.BLACK)
            animateY(1000)

            // Configure legend to prevent overlapping
            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                textSize = 10f
                formSize = 10f
                xEntrySpace = 7f
                yEntrySpace = 5f
                formToTextSpace = 5f
                isWordWrapEnabled = true
            }

            invalidate()
        }
    }

    private fun setupEmptyChart(chart: PieChart, message: String) {
        chart.apply {
            clear()
            setNoDataText(message)
            setNoDataTextColor(Color.GRAY)
            invalidate()
        }
    }
}
package com.afeka.smarttodoapp

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.Spinner
import com.afeka.smarttodoapp.data.Task
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class AddEditTaskDialog(
    private val context: Context,
    private val task: Task? = null,
    private val onSave: (Task) -> Unit
) {

    private lateinit var dialog: Dialog
    private lateinit var etTaskTitle: TextInputEditText
    private lateinit var etTaskDescription: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var rbHigh: RadioButton
    private lateinit var rbMedium: RadioButton
    private lateinit var rbLow: RadioButton
    private lateinit var btnSelectDate: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton

    private var selectedDueDate: Long? = null

    private val categories = arrayOf("General", "Work", "Personal", "Shopping", "Study")

    // Initialize and show the dialog
    fun show() {
        dialog = Dialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_edit_task, null)
        dialog.setContentView(view)

        initViews(view)
        setupCategorySpinner()

        if (task != null) {
            fillTaskData(task)
        }

        setupListeners()

        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog.show()
    }

    //Connect XML to variables
    private fun initViews(view: android.view.View) {
        etTaskTitle = view.findViewById(R.id.etTaskTitle)
        etTaskDescription = view.findViewById(R.id.etTaskDescription)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        rbHigh = view.findViewById(R.id.rbHigh)
        rbMedium = view.findViewById(R.id.rbMedium)
        rbLow = view.findViewById(R.id.rbLow)
        btnSelectDate = view.findViewById(R.id.btnSelectDate)
        btnSave = view.findViewById(R.id.btnSave)
        btnCancel = view.findViewById(R.id.btnCancel)
    }

    // Setup category spinner to show option
    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    // Fill edit mode - mission
    private fun fillTaskData(task: Task) {
        etTaskTitle.setText(task.title)
        etTaskDescription.setText(task.description)

        val categoryPosition = categories.indexOf(task.category)
        if (categoryPosition >= 0) {
            spinnerCategory.setSelection(categoryPosition)
        }

        // Set priority radio button based on String value
        when (task.priority) {
            "HIGH" -> rbHigh.isChecked = true
            "MEDIUM" -> rbMedium.isChecked = true
            "LOW" -> rbLow.isChecked = true
        }

        selectedDueDate = task.dueDate
        if (selectedDueDate != null) {
            updateDateButtonText(selectedDueDate!!)
        }
    }

    // Setup buttons click to select,cancel and save
    private fun setupListeners() {
        btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            saveTask()
        }
    }

    // choose day option for mission
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        if (selectedDueDate != null) {
            calendar.timeInMillis = selectedDueDate!!
        }

        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDueDate = calendar.timeInMillis
                updateDateButtonText(selectedDueDate!!)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }

    // edit date txt to nice numbers
    private fun updateDateButtonText(date: Long) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        btnSelectDate.text = dateFormat.format(Date(date))
    }

    //updating or creating one
    private fun saveTask() {
        val title = etTaskTitle.text.toString().trim()

        if (title.isEmpty()) {
            etTaskTitle.error = "Title is required"
            return
        }

        val description = etTaskDescription.text.toString().trim()
        val category = spinnerCategory.selectedItem.toString()

        // Convert priority to String for Firestore
        val priority = when {
            rbHigh.isChecked -> "HIGH"
            rbMedium.isChecked -> "MEDIUM"
            rbLow.isChecked -> "LOW"
            else -> "MEDIUM"
        }

        val newTask = if (task != null) {
            // Edit existing task
            task.copy(
                title = title,
                description = description,
                category = category,
                priority = priority,
                dueDate = selectedDueDate
            )
        } else {
            // Create new task (userId will be set by repository)
            Task(
                title = title,
                description = description,
                category = category,
                priority = priority,
                dueDate = selectedDueDate
            )
        }

        onSave(newTask)
        dialog.dismiss()
    }
}
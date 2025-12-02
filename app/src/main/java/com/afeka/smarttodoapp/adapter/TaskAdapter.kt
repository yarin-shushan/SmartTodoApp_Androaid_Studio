package com.afeka.smarttodoapp.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.afeka.smarttodoapp.R
import com.afeka.smarttodoapp.data.Task
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val onTaskClick: (Task) -> Unit,
    private val onTaskLongClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit,
    private val onCheckboxClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task)
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkboxCompleted: CheckBox = itemView.findViewById(R.id.checkboxCompleted)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvPriority: TextView = itemView.findViewById(R.id.tvPriority)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvDueDate: TextView = itemView.findViewById(R.id.tvDueDate)
        private val tvOverdue: TextView = itemView.findViewById(R.id.tvOverdue)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(task: Task) {
            // Head line
            tvTitle.text = task.title

            // description
            if (task.description.isNotEmpty()) {
                tvDescription.text = task.description
                tvDescription.visibility = View.VISIBLE
            } else {
                tvDescription.visibility = View.GONE
            }

            // Category
            tvCategory.text = "📁 ${task.category}"

            // Priority - shortened text (now String instead of enum)
            tvPriority.text = when (task.priority) {
                "HIGH" -> "HIGH"
                "MEDIUM" -> "MED"
                "LOW" -> "LOW"
                else -> "MED"
            }

            // Priority color (now String comparison)
            val priorityColor = when (task.priority) {
                "HIGH" -> android.R.color.holo_red_light
                "MEDIUM" -> android.R.color.holo_orange_light
                "LOW" -> android.R.color.holo_green_light
                else -> android.R.color.holo_orange_light
            }
            tvPriority.setBackgroundColor(
                ContextCompat.getColor(itemView.context, priorityColor)
            )

            // Due Date
            if (task.dueDate != null) {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val dateText = dateFormat.format(Date(task.dueDate))

                val currentTime = System.currentTimeMillis()
                val isOverdue = task.dueDate < currentTime && !task.isCompleted

                // Show date
                tvDueDate.text = "📅 $dateText"
                tvDueDate.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
                tvDueDate.visibility = View.VISIBLE

                // Show overdue warning separately
                if (isOverdue) {
                    tvOverdue.text = "⚠️ באיחור!"
                    tvOverdue.visibility = View.VISIBLE
                } else {
                    tvOverdue.visibility = View.GONE
                }
            } else {
                tvDueDate.visibility = View.GONE
                tvOverdue.visibility = View.GONE
            }

            // checkbox
            checkboxCompleted.isChecked = task.isCompleted

            // cross line for complish
            if (task.isCompleted) {
                tvTitle.paintFlags = tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvTitle.alpha = 0.5f
            } else {
                tvTitle.paintFlags = tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                tvTitle.alpha = 1.0f
            }

            // clicks
            itemView.setOnClickListener { onTaskClick(task) }
            itemView.setOnLongClickListener {
                onTaskLongClick(task)
                true
            }
            btnDelete.setOnClickListener { onDeleteClick(task) }
            checkboxCompleted.setOnClickListener { onCheckboxClick(task) }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}
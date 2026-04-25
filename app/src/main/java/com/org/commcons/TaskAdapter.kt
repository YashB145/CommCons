package com.org.commcons

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private var tasks: List<Task>,
    private val isNgo: Boolean,
    private val onAcceptClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit,
    private val onItemClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvTaskDescription)
        val tvPriority: TextView = itemView.findViewById(R.id.tvPriority)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        holder.tvTitle.text = task.title
        holder.tvDescription.text = task.description
        holder.tvLocation.text = if (task.locationName.isNotEmpty())
            "📍 ${task.locationName}" else "📍 Location not specified"
        holder.tvStatus.text = task.status.uppercase()
        holder.tvPriority.text = task.priority.uppercase()

        val priorityColor = when (task.priority) {
            "high" -> android.graphics.Color.parseColor("#F44336")
            "medium" -> android.graphics.Color.parseColor("#FF9800")
            else -> android.graphics.Color.parseColor("#4CAF50")
        }
        holder.tvPriority.setBackgroundColor(priorityColor)

        if (isNgo) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnAccept.visibility = View.GONE
        } else {
            holder.btnAccept.visibility = View.VISIBLE
            holder.btnDelete.visibility = View.GONE
        }

        holder.btnAccept.setOnClickListener { onAcceptClick(task) }
        holder.btnDelete.setOnClickListener { onDeleteClick(task) }
        holder.itemView.setOnClickListener { onItemClick(task) }
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}
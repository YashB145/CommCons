package com.org.commcons

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
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
        val ctx = holder.itemView.context

        holder.tvTitle.text = task.title
        holder.tvDescription.text = task.description
        // Layout now shows a location pin icon drawable; text needs no emoji prefix
        holder.tvLocation.text = if (task.locationName.isNotEmpty())
            task.locationName else ctx.getString(R.string.location_not_specified)
        holder.tvStatus.text = task.status.uppercase()
        holder.tvPriority.text = task.priority.uppercase()

        // Use color resources so both light and dark themes render correctly
        val (textColorRes, bgColorRes) = when (task.priority) {
            "high"   -> Pair(R.color.sl_priority_high,   R.color.sl_priority_high_bg)
            "medium" -> Pair(R.color.sl_priority_medium, R.color.sl_priority_medium_bg)
            else     -> Pair(R.color.sl_priority_low,    R.color.sl_priority_low_bg)
        }
        holder.tvPriority.setTextColor(ContextCompat.getColor(ctx, textColorRes))
        val chipBg = ContextCompat.getDrawable(ctx, R.drawable.sl_chip_bg)?.mutate()
        chipBg?.let {
            DrawableCompat.setTint(it, ContextCompat.getColor(ctx, bgColorRes))
            holder.tvPriority.background = it
        }

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
package com.org.commcons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.org.commcons.databinding.ActivityTaskHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class TaskHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskHistoryBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val uid get() = auth.currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val isNgo = intent.getBooleanExtra("isNgo", false)
        binding.tvTitle.text = if (isNgo) "Completed tasks" else "My task history"

        loadHistory(isNgo)
    }

    private fun loadHistory(isNgo: Boolean) {
        binding.progressBar.visibility = View.VISIBLE

        val query = if (isNgo) {
            db.collection("tasks")
                .whereEqualTo("ngoId", uid)
                .whereEqualTo("status", "done")
        } else {
            db.collection("tasks")
                .whereEqualTo("status", "done")
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                binding.progressBar.visibility = View.GONE
                val tasks = snapshot.documents
                    .mapNotNull { it.toObject(Task::class.java) }
                    .sortedByDescending { it.completedAt }

                if (tasks.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.rvHistory.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.rvHistory.visibility = View.VISIBLE
                    setupRecyclerView(tasks)
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.text = "Error loading history"
                binding.tvEmpty.visibility = View.VISIBLE
            }
    }

    private fun setupRecyclerView(tasks: List<Task>) {
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            inner class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
                val tvTitle: TextView = view.findViewById(R.id.tvHistoryTitle)
                val tvDate: TextView = view.findViewById(R.id.tvHistoryDate)
                val tvPriority: TextView = view.findViewById(R.id.tvHistoryPriority)
                val tvLocation: TextView = view.findViewById(R.id.tvHistoryLocation)
                val tvSkills: TextView = view.findViewById(R.id.tvHistorySkills)
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                HistoryViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_history, parent, false)
                )

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val task = tasks[position]
                holder as HistoryViewHolder
                val ctx = holder.itemView.context

                holder.tvTitle.text = task.title
                holder.tvPriority.text = task.priority.uppercase()

                // Layout shows location icon; no emoji prefix needed
                holder.tvLocation.text = task.locationName.ifEmpty { "No location" }
                // Layout shows skills icon; no emoji prefix needed
                holder.tvSkills.text = if (task.requiredSkills.isEmpty()) "No skills required"
                    else task.requiredSkills.joinToString(", ")

                // PRESENTATION: replace 0xFF hex ints with color resources
                val (textColorRes, bgColorRes) = when (task.priority) {
                    "high"   -> Pair(R.color.sl_priority_high,   R.color.sl_priority_high_bg)
                    "medium" -> Pair(R.color.sl_priority_medium, R.color.sl_priority_medium_bg)
                    else     -> Pair(R.color.sl_priority_low,    R.color.sl_priority_low_bg)
                }
                holder.tvPriority.setTextColor(ContextCompat.getColor(ctx, textColorRes))
                val chipBg = ContextCompat.getDrawable(ctx, R.drawable.sl_chip_bg)?.mutate()
                chipBg?.let {
                    androidx.core.graphics.drawable.DrawableCompat.setTint(
                        it, ContextCompat.getColor(ctx, bgColorRes)
                    )
                    holder.tvPriority.background = it
                }

                // Date - removed emoji ✅ prefix (layout now shows check icon)
                holder.tvDate.text = if (task.completedAt > 0) {
                    "Completed: " + SimpleDateFormat(
                        "dd MMM yyyy, hh:mm a", Locale.getDefault()
                    ).format(Date(task.completedAt))
                } else {
                    "Completed"
                }
            }

            override fun getItemCount() = tasks.size
        }
    }
}
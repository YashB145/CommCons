package com.org.commcons

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.org.commcons.databinding.ActivityTaskDetailBinding

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val repo = TaskRepository()
    private var taskId = ""
    private var userRole = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        taskId = intent.getStringExtra("taskId") ?: ""
        userRole = intent.getStringExtra("userRole") ?: "volunteer"

        binding.btnBack.setOnClickListener { finish() }

        if (taskId.isNotEmpty()) {
            loadTask()
        } else {
            Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadTask() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("tasks").document(taskId).get()
            .addOnSuccessListener { doc ->
                binding.progressBar.visibility = View.GONE
                val task = doc.toObject(Task::class.java) ?: return@addOnSuccessListener
                bindTask(task)
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load task", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun bindTask(task: Task) {
        binding.tvDetailTitle.text = task.title
        binding.tvDetailDescription.text = task.description
        binding.tvDetailLocation.text = "📍 ${task.locationName.ifEmpty { "Not specified" }}"
        binding.tvDetailStatus.text = "Status: ${task.status.uppercase()}"
        binding.tvDetailSkills.text = "🛠 Skills: ${
            if (task.requiredSkills.isEmpty()) "None required"
            else task.requiredSkills.joinToString(", ")
        }"

        binding.tvDetailPriority.text = task.priority.uppercase()
        val color = when (task.priority) {
            "high" -> android.graphics.Color.parseColor("#F44336")
            "medium" -> android.graphics.Color.parseColor("#FF9800")
            else -> android.graphics.Color.parseColor("#4CAF50")
        }
        binding.tvDetailPriority.setBackgroundColor(color)

        binding.tvDetailStatus.setTextColor(
            when (task.status) {
                "open" -> android.graphics.Color.parseColor("#1565C0")
                "in_progress" -> android.graphics.Color.parseColor("#FF9800")
                "done" -> android.graphics.Color.parseColor("#4CAF50")
                else -> android.graphics.Color.parseColor("#888888")
            }
        )

        when (userRole) {
            "ngo", "admin" -> {
                binding.btnAcceptTask.visibility = View.GONE
                binding.btnDeleteTask.visibility = View.VISIBLE
                binding.btnMarkDone.visibility =
                    if (task.status == "in_progress") View.VISIBLE else View.GONE
            }
            "volunteer" -> {
                binding.btnDeleteTask.visibility = View.GONE
                binding.btnAcceptTask.visibility =
                    if (task.status == "open") View.VISIBLE else View.GONE
                binding.btnMarkDone.visibility =
                    if (task.status == "in_progress") View.VISIBLE else View.GONE
            }
        }

        binding.btnAcceptTask.setOnClickListener {
            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            binding.btnAcceptTask.isEnabled = false
            repo.acceptTask(task.id, uid,
                onSuccess = {
                    repo.updateTaskStatus(task.id, "in_progress", {
                        Toast.makeText(this, "✅ Task accepted!", Toast.LENGTH_SHORT).show()
                        loadTask()
                    }, { e -> showError(e) })
                },
                onFailure = { e ->
                    binding.btnAcceptTask.isEnabled = true
                    showError(e)
                }
            )
        }

        binding.btnMarkDone.setOnClickListener {
            binding.btnMarkDone.isEnabled = false
            repo.updateTaskStatus(task.id, "done", {
                Toast.makeText(this, "🎉 Marked as done!", Toast.LENGTH_SHORT).show()
                loadTask()
            }, { e ->
                binding.btnMarkDone.isEnabled = true
                showError(e)
            })
        }

        binding.btnDeleteTask.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Delete \"${task.title}\"?")
                .setPositiveButton("Delete") { _, _ ->
                    repo.deleteTask(task.id,
                        onSuccess = {
                            Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show()
                            finish()
                        },
                        onFailure = { e -> showError(e) }
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showError(msg: String) {
        Toast.makeText(this, "Error: $msg", Toast.LENGTH_LONG).show()
    }
}
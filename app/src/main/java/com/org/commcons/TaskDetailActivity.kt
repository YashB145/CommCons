package com.org.commcons

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private var currentUserName = ""

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
        // Layout shows location icon; no emoji prefix needed
        binding.tvDetailLocation.text = task.locationName.ifEmpty { "Not specified" }
        binding.tvDetailStatus.text = "Status: ${task.status.uppercase()}"
        // Layout shows skills icon; no emoji prefix needed
        binding.tvDetailSkills.text = if (task.requiredSkills.isEmpty()) "None required"
            else task.requiredSkills.joinToString(", ")

        binding.tvDetailPriority.text = task.priority.uppercase()

        // PRESENTATION: replace Color.parseColor with color resources for theme correctness
        val (priorityTextRes, priorityBgRes) = when (task.priority) {
            "high"   -> Pair(R.color.sl_priority_high,   R.color.sl_priority_high_bg)
            "medium" -> Pair(R.color.sl_priority_medium, R.color.sl_priority_medium_bg)
            else     -> Pair(R.color.sl_priority_low,    R.color.sl_priority_low_bg)
        }
        binding.tvDetailPriority.setTextColor(ContextCompat.getColor(this, priorityTextRes))
        val chipBg = ContextCompat.getDrawable(this, R.drawable.sl_chip_bg)?.mutate()
        chipBg?.let {
            androidx.core.graphics.drawable.DrawableCompat.setTint(
                it, ContextCompat.getColor(this, priorityBgRes)
            )
            binding.tvDetailPriority.background = it
        }

        // PRESENTATION: status text color via color resources
        binding.tvDetailStatus.setTextColor(
            when (task.status) {
                "open"        -> ContextCompat.getColor(this, R.color.sl_status_open)
                "in_progress" -> ContextCompat.getColor(this, R.color.sl_status_progress)
                "done"        -> ContextCompat.getColor(this, R.color.sl_status_done)
                else          -> ContextCompat.getColor(this, R.color.sl_text_secondary)
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

            db.collection("users").document(uid).get()
                .addOnSuccessListener { userDoc ->
                    currentUserName = userDoc.getString("name") ?: "A volunteer"

                    repo.acceptTask(task.id, uid,
                        onSuccess = {
                            repo.updateTaskStatus(task.id, "in_progress", {
                                Toast.makeText(this, "Task accepted", Toast.LENGTH_SHORT).show()
                                NotificationHelper.sendNotificationToUser(
                                    recipientUid = task.ngoId,
                                    title = "Task Accepted",
                                    body = "$currentUserName accepted: ${task.title}"
                                )
                                loadTask()
                            }, { e -> showError(e) })
                        },
                        onFailure = { e ->
                            binding.btnAcceptTask.isEnabled = true
                            showError(e)
                        }
                    )
                }
                .addOnFailureListener {
                    binding.btnAcceptTask.isEnabled = true
                    showError("Could not load user profile")
                }
        }

        binding.btnMarkDone.setOnClickListener {
            binding.btnMarkDone.isEnabled = false
            repo.updateTaskStatus(task.id, "done", {
                Toast.makeText(this, "Marked as done", Toast.LENGTH_SHORT).show()
                loadTask()
            }, { e ->
                binding.btnMarkDone.isEnabled = true
                showError(e)
            })
        }

        binding.btnDeleteTask.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete task")
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
package com.org.commcons

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.org.commcons.databinding.ActivityCreateTaskBinding

class CreateTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateTaskBinding
    private val auth = FirebaseAuth.getInstance()
    private val repo = TaskRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnSubmitTask.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val location = binding.etLocation.text.toString().trim()
            val skillsRaw = binding.etSkills.text.toString().trim()

            if (title.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Title and Description are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val priority = when (binding.priorityGroup.checkedRadioButtonId) {
                R.id.rbHigh -> "high"
                R.id.rbMedium -> "medium"
                else -> "low"
            }


            val skills = if (skillsRaw.isEmpty()) emptyList()
            else skillsRaw.split(",").map { it.trim() }

            val ngoId = auth.currentUser?.uid ?: return@setOnClickListener

            binding.progressBar.visibility = View.VISIBLE
            binding.btnSubmitTask.isEnabled = false

            val task = Task(
                ngoId = ngoId,
                title = title,
                description = description,
                locationName = location,
                requiredSkills = skills,
                priority = priority,
                status = "open"
            )

            repo.createTask(task,
                onSuccess = {

                    binding.progressBar.visibility = View.GONE

                    Toast.makeText(this, "✅ Task created!", Toast.LENGTH_SHORT).show()

                    finish()
                    // Notify all volunteers about new task
                    notifyAllVolunteers(title)
                },
                onFailure = { error ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmitTask.isEnabled = true
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                }
            )
            private fun notifyAllVolunteers(taskTitle: String) {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("users")
                    .whereEqualTo("role", "volunteer")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        for (doc in snapshot.documents) {
                            val uid = doc.id
                            NotificationHelper.sendNotificationToUser(
                                recipientUid = uid,
                                title = "New Task Available!",
                                body = "Check out: $taskTitle"
                            )
                        }
                    }
            }
        }
    }
}
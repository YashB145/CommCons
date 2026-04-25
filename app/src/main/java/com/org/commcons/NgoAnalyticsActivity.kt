package com.org.commcons

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.org.commcons.databinding.ActivityNgoAnalyticsBinding

class NgoAnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNgoAnalyticsBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val uid get() = auth.currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNgoAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        loadAnalytics()
    }

    private fun loadAnalytics() {
        // Total tasks created by this NGO
        db.collection("tasks")
            .whereEqualTo("ngoId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val tasks = snapshot.documents.mapNotNull { it.toObject(Task::class.java) }
                binding.tvTotalTasks.text = tasks.size.toString()

                val open = tasks.count { it.status == "open" }
                val inProgress = tasks.count { it.status == "in_progress" }
                val completed = tasks.count { it.status == "completed" }

                binding.tvOpenTasks.text = open.toString()
                binding.tvInProgressTasks.text = inProgress.toString()
                binding.tvCompletedTasks.text = completed.toString()

                // Impact score
                val impact = (completed * 10) + (inProgress * 5)
                binding.tvImpactScore.text = impact.toString()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        // Total volunteers engaged
        db.collection("users")
            .whereEqualTo("role", "volunteer")
            .get()
            .addOnSuccessListener { snapshot ->
                binding.tvTotalVolunteers.text = snapshot.size().toString()
            }

        // Surveys created
        db.collection("surveys")
            .whereEqualTo("ngoId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.tvTotalSurveys.text = snapshot.size().toString()
            }
    }
}
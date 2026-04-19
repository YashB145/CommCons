package com.org.commcons

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.org.commcons.databinding.ActivityNgoDashboardBinding

class NgoDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNgoDashboardBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var userName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNgoDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserData()

        binding.btnCreateTask.setOnClickListener {
            Toast.makeText(this, "Create Task - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnCreateSurvey.setOnClickListener {
            Toast.makeText(this, "Create Survey - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnViewAnalytics.setOnClickListener {
            Toast.makeText(this, "Analytics - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnVolunteers.setOnClickListener {
            Toast.makeText(this, "Volunteers - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                userName = doc.getString("name") ?: "NGO"
                binding.tvWelcome.text = "Welcome, $userName!"
                loadStats()
            }
    }

    private fun loadStats() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("tasks")
            .whereEqualTo("ngoId", uid)
            .get()
            .addOnSuccessListener { tasks ->
                binding.tvTaskCount.text = tasks.size().toString()
            }

        db.collection("surveys")
            .whereEqualTo("ngoId", uid)
            .get()
            .addOnSuccessListener { surveys ->
                binding.tvSurveyCount.text = surveys.size().toString()
            }

        db.collection("users")
            .whereEqualTo("role", "volunteer")
            .get()
            .addOnSuccessListener { volunteers ->
                binding.tvVolunteerCount.text = volunteers.size().toString()
            }
    }
}
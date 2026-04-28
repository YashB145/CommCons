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
        SettingsActivity.applyTheme(this)
        val uid = auth.currentUser?.uid ?: ""

        // Debug toasts

        NotificationHelper.checkAndShowNotifications(this, uid)

        loadUserData()

        binding.btnCreateTask.setOnClickListener {
            startActivity(Intent(this, CreateTaskActivity::class.java))
        }
        binding.btnCreateSurvey.setOnClickListener {
            startActivity(Intent(this, ChatListActivity::class.java))
        }
        binding.btnViewAnalytics.setOnClickListener {
            startActivity(Intent(this, NgoAnalyticsActivity::class.java))
        }
        binding.btnVolunteers.setOnClickListener {
            val intent = Intent(this, TaskListActivity::class.java)
            intent.putExtra("isNgo", true)
            startActivity(intent)
        }
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        binding.btnHistory.setOnClickListener {
            val i = Intent(this, TaskHistoryActivity::class.java)
            i.putExtra("isNgo", true)
            startActivity(i)
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
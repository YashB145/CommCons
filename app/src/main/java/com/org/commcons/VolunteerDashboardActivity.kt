package com.org.commcons

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.org.commcons.databinding.ActivityVolunteerDashboardBinding

class VolunteerDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVolunteerDashboardBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVolunteerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserData()

        binding.btnViewTasks.setOnClickListener {
            val i = Intent(this, TaskListActivity::class.java)
            i.putExtra("isNgo", false)
            startActivity(i)
        }

        binding.btnViewMap.setOnClickListener {
            Toast.makeText(this, "Map - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnChat.setOnClickListener {
            startActivity(Intent(this, ChatListActivity::class.java))
        }

        binding.btnProfile.setOnClickListener {
            Toast.makeText(this, "Profile - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val i = Intent(this, LoginActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
            finish()
        }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "Volunteer"
                binding.tvWelcome.text = "Welcome, $name!"
                loadStats(uid)
            }
    }

    private fun loadStats(uid: String) {
        db.collection("volunteersAssigned")
            .whereEqualTo("volunteerId", uid)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { docs ->
                binding.tvTasksAccepted.text = docs.size().toString()
            }

        db.collection("tasks")
            .whereEqualTo("status", "open")
            .get()
            .addOnSuccessListener { docs ->
                binding.tvTasksAvailable.text = docs.size().toString()
            }
    }
}
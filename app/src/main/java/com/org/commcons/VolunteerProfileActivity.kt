package com.org.commcons

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.org.commcons.databinding.ActivityVolunteerProfileBinding

class VolunteerProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVolunteerProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val uid get() = auth.currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVolunteerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { saveProfile() }

        loadProfile()
        loadStats()
    }

    private fun loadProfile() {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                binding.etName.setText(doc.getString("name") ?: "")
                binding.etBio.setText(doc.getString("bio") ?: "")
                val skills = (doc.get("skills") as? List<*>)?.joinToString(", ") ?: ""
                binding.etSkills.setText(skills)
            }
    }

    private fun loadStats() {
        // Tasks accepted
        db.collection("tasks")
            .whereArrayContains("acceptedBy", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.tvTasksAccepted.text = snapshot.size().toString()
            }

        // Tasks completed
        db.collection("tasks")
            .whereEqualTo("completedBy", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.tvTasksCompleted.text = snapshot.size().toString()
            }

        // Surveys filled
        db.collection("surveyResponses")
            .whereEqualTo("respondentId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.tvSurveysFilled.text = snapshot.size().toString()
            }
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()
        val skillsRaw = binding.etSkills.text.toString().trim()
        val skills = skillsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        if (name.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSave.isEnabled = false

        val updates = hashMapOf<String, Any>(
            "name" to name,
            "bio" to bio,
            "skills" to skills
        )

        db.collection("users").document(uid).update(updates)
            .addOnSuccessListener {
                binding.btnSave.isEnabled = true
                Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                binding.btnSave.isEnabled = true
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
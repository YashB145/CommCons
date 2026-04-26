package com.org.commcons

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.org.commcons.databinding.ActivityMatchBinding

class MatchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMatchBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val uid get() = auth.currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMatchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Toast.makeText(this, "Match screen loaded", Toast.LENGTH_SHORT).show()
        binding.btnBack.setOnClickListener { finish() }

        showStatus("Loading tasks...")
        loadMatches()
    }

    private fun showStatus(msg: String) {
        binding.tvStatus.text = msg
        binding.tvStatus.visibility = View.VISIBLE
        binding.rvMatches.visibility = View.GONE
    }

    private fun showList() {
        binding.tvStatus.visibility = View.GONE
        binding.rvMatches.visibility = View.VISIBLE
    }

    private fun loadMatches() {
        // Step 1: get volunteer skills
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val volunteerSkills = (userDoc.get("skills") as? List<*>)
                    ?.mapNotNull { it?.toString()?.lowercase()?.trim() }
                    ?: emptyList()

                showStatus("Finding tasks for skills: ${
                    if (volunteerSkills.isEmpty()) "none set"
                    else volunteerSkills.joinToString(", ")
                }")

                // Step 2: get ALL tasks (not just open, to debug)
                db.collection("tasks").get()
                    .addOnSuccessListener { snapshot ->
                        val allTasks = snapshot.documents.mapNotNull {
                            it.toObject(Task::class.java)
                        }

                        if (allTasks.isEmpty()) {
                            showStatus("No tasks found in database at all")
                            return@addOnSuccessListener
                        }

                        // Step 3: filter open + score
                        val openTasks = allTasks.filter { it.status == "open" }

                        if (openTasks.isEmpty()) {
                            showStatus("No open tasks found. Total tasks: ${allTasks.size}")
                            return@addOnSuccessListener
                        }

                        val scored = openTasks.map { task ->
                            val taskSkills = task.requiredSkills
                                .map { it.lowercase().trim() }
                            val score = when {
                                taskSkills.isEmpty() -> 50
                                volunteerSkills.isEmpty() -> 30
                                else -> {
                                    val matched = taskSkills
                                        .count { it in volunteerSkills }
                                    if (matched == 0) 20
                                    else (matched.toFloat() / taskSkills.size * 100).toInt()
                                }
                            }
                            Pair(task, score)
                        }.sortedByDescending { it.second }

                        showList()
                        setupRecyclerView(scored, volunteerSkills)
                    }
                    .addOnFailureListener { e ->
                        showStatus("Firestore error: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                showStatus("Could not load user profile: ${e.message}")
            }
    }

    private fun setupRecyclerView(
        matches: List<Pair<Task, Int>>,
        volunteerSkills: List<String>
    ) {
        binding.rvMatches.layoutManager = LinearLayoutManager(this)
        binding.rvMatches.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            inner class MatchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
                val tvTitle: TextView = view.findViewById(R.id.tvTaskTitle)
                val tvScore: TextView = view.findViewById(R.id.tvMatchScore)
                val tvSkills: TextView = view.findViewById(R.id.tvRequiredSkills)
                val tvPriority: TextView = view.findViewById(R.id.tvPriority)
                val tvLocation: TextView = view.findViewById(R.id.tvLocation)
                val tvMatchedSkills: TextView = view.findViewById(R.id.tvMatchedSkills)
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                MatchViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_match, parent, false)
                )

            override fun onBindViewHolder(
                holder: RecyclerView.ViewHolder, position: Int
            ) {
                val (task, score) = matches[position]
                holder as MatchViewHolder

                holder.tvTitle.text = task.title
                holder.tvScore.text = "$score%"
                holder.tvScore.setTextColor(
                    when {
                        score >= 80 -> 0xFF2E7D32.toInt()
                        score >= 50 -> 0xFFF57C00.toInt()
                        else -> 0xFF1565C0.toInt()
                    }
                )

                val taskSkills = task.requiredSkills
                    .map { it.lowercase().trim() }
                val matched = taskSkills.filter { it in volunteerSkills }

                holder.tvSkills.text = "Required: ${
                    if (taskSkills.isEmpty()) "Open to all"
                    else taskSkills.joinToString(", ")
                }"
                holder.tvMatchedSkills.text = "✓ You have: ${
                    if (matched.isEmpty()) "—"
                    else matched.joinToString(", ")
                }"
                holder.tvPriority.text = "Priority: ${task.priority.uppercase()}"
                holder.tvLocation.text = "📍 ${
                    task.locationName.ifEmpty { "Location TBD" }
                }"

                holder.itemView.setOnClickListener {
                    val intent = Intent(
                        this@MatchActivity,
                        TaskDetailActivity::class.java
                    )
                    intent.putExtra("taskId", task.id)
                    intent.putExtra("userRole", "volunteer")
                    startActivity(intent)
                }
            }

            override fun getItemCount() = matches.size
        }
    }
}
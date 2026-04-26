package com.org.commcons

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

        binding.btnBack.setOnClickListener { finish() }
        loadMatches()
    }

    private fun loadMatches() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "Finding best matches..."

        // Step 1: Get current volunteer's skills
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val volunteerName = userDoc.getString("name") ?: "Volunteer"
                val volunteerSkills = (userDoc.get("skills") as? List<*>)
                    ?.mapNotNull { it?.toString()?.lowercase()?.trim() }
                    ?: emptyList()

                if (volunteerSkills.isEmpty()) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvStatus.text = "Add skills to your profile first!"
                    binding.tvStatus.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                // Step 2: Get all open tasks
                db.collection("tasks")
                    .whereEqualTo("status", "open")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val tasks = snapshot.documents
                            .mapNotNull { it.toObject(Task::class.java) }

                        // Step 3: Score each task by skill match
                        val scored = tasks.mapNotNull { task ->
                            val taskSkills = task.requiredSkills
                                .map { it.lowercase().trim() }
                            val matchCount = taskSkills.count { it in volunteerSkills }
                            val score = when {
                                taskSkills.isEmpty() -> 50 // no skill req = open to all
                                matchCount == 0 -> 0
                                else -> (matchCount.toFloat() / taskSkills.size * 100).toInt()
                            }
                            if (score > 0) Pair(task, score) else null
                        }.sortedByDescending { it.second }

                        binding.progressBar.visibility = View.GONE

                        if (scored.isEmpty()) {
                            binding.tvStatus.text =
                                "No matching tasks found for skills: ${volunteerSkills.joinToString(", ")}"
                            binding.tvStatus.visibility = View.VISIBLE
                        } else {
                            binding.tvStatus.visibility = View.GONE
                            setupRecyclerView(scored, volunteerSkills)
                        }
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
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

                val taskSkills = task.requiredSkills.map { it.lowercase().trim() }
                val matched = taskSkills.filter { it in volunteerSkills }
                val unmatched = taskSkills.filter { it !in volunteerSkills }

                holder.tvSkills.text = "Required: ${
                    if (taskSkills.isEmpty()) "None" else taskSkills.joinToString(", ")
                }"
                holder.tvMatchedSkills.text = "✓ You have: ${
                    if (matched.isEmpty()) "—" else matched.joinToString(", ")
                }"
                holder.tvPriority.text = "Priority: ${task.priority.uppercase()}"
                holder.tvLocation.text = "📍 ${task.locationName.ifEmpty { "Location TBD" }}"

                holder.itemView.setOnClickListener {
                    val intent = android.content.Intent(
                        this@MatchActivity, TaskDetailActivity::class.java
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
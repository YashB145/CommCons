package com.org.commcons

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.org.commcons.databinding.ActivityTaskListBinding

class TaskListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskListBinding
    private val auth = FirebaseAuth.getInstance()
    private val repo = TaskRepository()
    private lateinit var adapter: TaskAdapter
    private var isNgo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isNgo = intent.getBooleanExtra("isNgo", false)
        binding.tvTitle.text = if (isNgo) "My Tasks" else "Available Tasks"

        setupRecyclerView()
        loadTasks()

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = TaskAdapter(
            tasks = emptyList(),
            isNgo = isNgo,
            onAcceptClick = { task -> acceptTask(task) },
            onDeleteClick = { task -> deleteTask(task) },
            onItemClick = { task ->
                val i = Intent(this, TaskDetailActivity::class.java)
                i.putExtra("taskId", task.id)
                i.putExtra("userRole", if (isNgo) "ngo" else "volunteer")
                startActivity(i)
            }
        )
        binding.rvTasks.layoutManager = LinearLayoutManager(this)
        binding.rvTasks.adapter = adapter
    }

    private fun loadTasks() {
        val uid = auth.currentUser?.uid ?: return

        if (isNgo) {
            repo.getNgoTasks(uid,
                onSuccess = { tasks ->
                    adapter.updateTasks(tasks)
                    binding.tvEmpty.visibility =
                        if (tasks.isEmpty()) View.VISIBLE else View.GONE
                },
                onFailure = { error ->
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                }
            )
        } else {
            repo.getOpenTasks(
                onSuccess = { tasks ->
                    adapter.updateTasks(tasks)
                    binding.tvEmpty.visibility =
                        if (tasks.isEmpty()) View.VISIBLE else View.GONE
                },
                onFailure = { error ->
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun acceptTask(task: Task) {
        val uid = auth.currentUser?.uid ?: return
        repo.acceptTask(task.id, uid,
            onSuccess = {
                repo.updateTaskStatus(task.id, "in_progress", {
                    Toast.makeText(this, "✅ Task accepted!", Toast.LENGTH_SHORT).show()
                }, { e -> Toast.makeText(this, "Error: $e", Toast.LENGTH_LONG).show() })
            },
            onFailure = { e ->
                Toast.makeText(this, "Error: $e", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun deleteTask(task: Task) {
        repo.deleteTask(task.id,
            onSuccess = {
                Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show()
            },
            onFailure = { e ->
                Toast.makeText(this, "Error: $e", Toast.LENGTH_LONG).show()
            }
        )
    }
}
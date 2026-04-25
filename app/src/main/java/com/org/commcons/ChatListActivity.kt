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
import com.org.commcons.databinding.ActivityChatListBinding

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var currentUserId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = auth.currentUser?.uid ?: ""

        binding.btnBack.setOnClickListener { finish() }

        loadUsers()
    }

    private fun loadUsers() {
        // Load all users except current user to start a chat with
        db.collection("users").get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.documents
                    .mapNotNull { doc ->
                        val uid = doc.id
                        val name = doc.getString("name") ?: return@mapNotNull null
                        val role = doc.getString("role") ?: ""
                        if (uid == currentUserId) null
                        else Triple(uid, name, role)
                    }

                if (users.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                binding.tvEmpty.visibility = View.GONE
                setupRecyclerView(users)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRecyclerView(users: List<Triple<String, String, String>>) {
        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
                val tvName: TextView = view.findViewById(R.id.tvUserName)
                val tvRole: TextView = view.findViewById(R.id.tvUserRole)
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_user, parent, false)
                return UserViewHolder(view)
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val (uid, name, role) = users[position]
                (holder as UserViewHolder).tvName.text = name
                holder.tvRole.text = role.uppercase()
                holder.itemView.setOnClickListener {
                    val intent = Intent(this@ChatListActivity, ChatActivity::class.java)
                    intent.putExtra("receiverId", uid)
                    intent.putExtra("receiverName", name)
                    startActivity(intent)
                }
            }

            override fun getItemCount() = users.size
        }
    }
}
package com.org.commcons

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.org.commcons.databinding.ActivityChatBinding

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: MessageAdapter

    private var currentUserId = ""
    private var currentUserName = ""
    private var receiverId = ""
    private var receiverName = ""
    private var chatId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        receiverId = intent.getStringExtra("receiverId") ?: ""
        receiverName = intent.getStringExtra("receiverName") ?: "User"
        currentUserId = auth.currentUser?.uid ?: ""

        binding.tvReceiverName.text = receiverName
        binding.btnBack.setOnClickListener { finish() }

        // Chat ID is always smaller UID + larger UID to keep it consistent
        chatId = if (currentUserId < receiverId)
            "${currentUserId}_${receiverId}"
        else
            "${receiverId}_${currentUserId}"

        loadCurrentUserName()
        setupRecyclerView()
        listenForMessages()

        binding.btnSend.setOnClickListener { sendMessage() }
    }

    private fun loadCurrentUserName() {
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { doc ->
                currentUserName = doc.getString("name") ?: "User"
            }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(emptyList(), currentUserId)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        binding.rvMessages.layoutManager = layoutManager
        binding.rvMessages.adapter = adapter
    }

    private fun listenForMessages() {
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull {
                    it.toObject(Message::class.java)
                } ?: emptyList()
                adapter.updateMessages(messages)
                if (messages.isNotEmpty()) {
                    binding.rvMessages.scrollToPosition(messages.size - 1)
                }
                binding.tvEmpty.visibility =
                    if (messages.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) return

        binding.btnSend.isEnabled = false

        val msgRef = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .document()

        val message = Message(
            id = msgRef.id,
            senderId = currentUserId,
            receiverId = receiverId,
            senderName = currentUserName,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        msgRef.set(message)
            .addOnSuccessListener {
                binding.etMessage.setText("")
                binding.btnSend.isEnabled = true
                // Save chat summary for chat list
                saveChatSummary(text)
            }
            .addOnFailureListener { e ->
                binding.btnSend.isEnabled = true
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveChatSummary(lastMessage: String) {
        val summary = hashMapOf(
            "chatId" to chatId,
            "lastMessage" to lastMessage,
            "timestamp" to System.currentTimeMillis(),
            "user1Id" to currentUserId,
            "user1Name" to currentUserName,
            "user2Id" to receiverId,
            "user2Name" to receiverName
        )
        // Notify receiver of new message
        NotificationHelper.sendNotificationToUser(
            recipientUid = receiverId,
            title = "New message from $currentUserName",
            body = lastMessage
        )
        db.collection("chatSummaries").document(chatId).set(summary)
    }
}
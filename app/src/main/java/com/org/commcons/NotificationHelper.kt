package com.org.commcons

import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object NotificationHelper {

    private val db = FirebaseFirestore.getInstance()

    // Send notification to a specific user by their UID
    fun sendNotificationToUser(
        recipientUid: String,
        title: String,
        body: String
    ) {
        // Get recipient's FCM token from Firestore
        db.collection("users").document(recipientUid).get()
            .addOnSuccessListener { doc ->
                val token = doc.getString("fcmToken") ?: return@addOnSuccessListener
                sendFCMNotification(token, title, body)
            }
    }

    private fun sendFCMNotification(token: String, title: String, body: String) {
        // Save notification to Firestore (reliable delivery)
        val notification = hashMapOf(
            "token" to token,
            "title" to title,
            "body" to body,
            "sentAt" to System.currentTimeMillis(),
            "delivered" to false
        )
        db.collection("notifications").add(notification)
    }

    // Save FCM token when user logs in
    fun saveFcmToken(uid: String) {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                db.collection("users").document(uid)
                    .update("fcmToken", token)
            }
    }
}
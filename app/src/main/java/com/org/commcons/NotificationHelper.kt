package com.org.commcons

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore

object NotificationHelper {

    private val db = FirebaseFirestore.getInstance()

    fun sendNotificationToUser(
        recipientUid: String,
        title: String,
        body: String
    ) {
        val notification = hashMapOf(
            "recipientUid" to recipientUid,
            "title" to title,
            "body" to body,
            "sentAt" to System.currentTimeMillis(),
            "read" to false
        )
        db.collection("notifications").add(notification)
    }

    fun saveFcmToken(uid: String) {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                db.collection("users").document(uid)
                    .set(mapOf("fcmToken" to token),
                        com.google.firebase.firestore.SetOptions.merge())
            }
    }

    fun checkAndShowNotifications(context: Context, uid: String) {
        db.collection("notifications")
            .whereEqualTo("recipientUid", uid)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val title = doc.getString("title") ?: continue
                    val body = doc.getString("body") ?: ""
                    showLocalNotification(context, title, body)
                    doc.reference.update("read", true)
                }
            }
    }

    private fun showLocalNotification(context: Context, title: String, body: String) {
        val channelId = "commcons_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "CommCons Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
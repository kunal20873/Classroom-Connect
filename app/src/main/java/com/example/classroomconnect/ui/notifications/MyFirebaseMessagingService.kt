package com.example.classroomconnect.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.classroomconnect.R
import com.example.classroomconnect.ui.classroom.ClassDetailActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        Log.d("FCM_MESSAGE", "FCM Service Created and Running")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        Log.d("FCM_MESSAGE", "--- New Message Received ---")
        Log.d("FCM_MESSAGE", "From: ${message.from}")
        Log.d("FCM_MESSAGE", "Data: ${message.data}")

        if (message.data.isNotEmpty()) {
            val title = message.data["title"] ?: "New Update"
            val body = message.data["body"] ?: "Check out the new classroom update!"
            val classId = message.data["classId"] ?: ""
            
            Log.d("FCM_MESSAGE", "Attempting to show notification: $title")
            showNotification(title, body, classId)
        }
        
        message.notification?.let {
            Log.d("FCM_MESSAGE", "Notification Payload: ${it.body}")
            showNotification(it.title ?: "Update", it.body ?: "", "")
        }
    }

    private fun showNotification(title: String, message: String, classId: String) {
        val channelId = "material_channel"
        val intent = Intent(this, ClassDetailActivity::class.java).apply {
            putExtra("ClassId", classId)
            putExtra("userRole", "Student")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val notificationId = System.currentTimeMillis().toInt()
        
        val pendingIntent = PendingIntent.getActivity(
            this, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        val channel = NotificationChannel(channelId, "Classroom Updates", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(notificationId, notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Refreshed token: $token")
    }
}

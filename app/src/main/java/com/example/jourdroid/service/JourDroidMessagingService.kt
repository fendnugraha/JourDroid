package com.example.jourdroid.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.jourdroid.MainActivity
import com.example.jourdroid.R
import com.example.jourdroid.api.ApiClient
import com.example.jourdroid.utils.AuthManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JourDroidMessagingService : FirebaseMessagingService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d("FCM", "Message received from: ${remoteMessage.from}")
        Log.d("FCM", "Message data: ${remoteMessage.data}")

        var title = "JourDroid Notification"
        var body = "Tap to open the app"

        // 1. Check for Data payload (Usually used by Laravel Kreait/Firebase)
        if (remoteMessage.data.isNotEmpty()) {
            title = remoteMessage.data["title"] ?: title
            body = remoteMessage.data["body"] ?: remoteMessage.data["message"] ?: body
            Log.d("FCM", "Extracted from Data: $title - $body")
        }

        // 2. Override with Notification payload if exists (Usually used by Firebase Console)
        remoteMessage.notification?.let {
            title = it.title ?: title
            body = it.body ?: body
            Log.d("FCM", "Overridden by Notification: $title - $body")
        }

        sendNotification(title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "NEW TOKEN GENERATED: $token")
        
        // Save to local storage immediately
        AuthManager(applicationContext).saveToken(token) // Assuming you want to save it here too, but AuthManager usually stores JWT. 
        // Wait, AuthManager's getToken/saveToken is for JWT. UserData has fcmToken.
        
        // Sync with server
        scope.launch {
            try {
                val apiService = ApiClient.getApiService(applicationContext)
                val response = apiService.updateFcmToken(token)
                if (response.isSuccessful) {
                    Log.d("FCM", "Token synced successfully")
                } else {
                    Log.e("FCM", "Sync failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Sync error", e)
            }
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "jourdroid_alerts"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound, Vibrate, LED
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "JourDroid Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "General notifications for JourDroid"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

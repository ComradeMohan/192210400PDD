package com.simats.univault

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

const val  channelId = "notification_channel"
const val chanelName = "com.simats.univault"

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title
        val body = remoteMessage.notification?.body

        val notificationBuilder = NotificationCompat.Builder(this, "notice_channel")
            .setSmallIcon(R.drawable.ic_splash_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "notice_channel",
                "Notice Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        manager.notify(0, notificationBuilder.build())
    }
}

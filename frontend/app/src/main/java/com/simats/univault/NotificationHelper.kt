package com.simats.univault

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
	const val CHANNEL_NOTICE_ID = "notice_channel"
	const val CHANNEL_NOTICE_NAME = "Notices"

	const val CHANNEL_EVENTS_ID = "event_notifications"
	const val CHANNEL_EVENTS_NAME = "Event Notifications"

	fun ensureChannels(context: Context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
		val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

		val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
		val audioAttrs = AudioAttributes.Builder()
			.setUsage(AudioAttributes.USAGE_NOTIFICATION)
			.build()

		val notice = NotificationChannel(
			CHANNEL_NOTICE_ID,
			CHANNEL_NOTICE_NAME,
			NotificationManager.IMPORTANCE_HIGH
		).apply {
			description = "General notices and announcements"
			enableLights(true)
			lightColor = Color.BLUE
			enableVibration(true)
			setSound(defaultSound, audioAttrs)
		}

		val events = NotificationChannel(
			CHANNEL_EVENTS_ID,
			CHANNEL_EVENTS_NAME,
			NotificationManager.IMPORTANCE_HIGH
		).apply {
			description = "Scheduled events and reminders"
			enableLights(true)
			lightColor = Color.MAGENTA
			enableVibration(true)
			setSound(defaultSound, audioAttrs)
		}

		manager.createNotificationChannel(notice)
		manager.createNotificationChannel(events)
	}

	fun buildNotification(
		context: Context,
		channelId: String,
		title: String,
		message: String,
		pendingIntent: PendingIntent? = null,
		smallIconRes: Int
	): androidx.core.app.NotificationCompat.Builder {
		val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
		return NotificationCompat.Builder(context, channelId)
			.setSmallIcon(smallIconRes)
			.setContentTitle(title)
			.setContentText(message)
			.setAutoCancel(true)
			.setSound(sound)
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.apply {
				if (pendingIntent != null) setContentIntent(pendingIntent)
			}
	}
}

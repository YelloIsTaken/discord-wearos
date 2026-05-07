package com.yelloistaken.discordwearos.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.yelloistaken.discordwearos.MainActivity
import com.yelloistaken.discordwearos.data.models.Message

const val CHANNEL_ID_MESSAGES = "discord_messages"
const val CHANNEL_ID_SERVICE = "discord_service"
const val NOTIFICATION_ID_SERVICE = 1

const val ACTION_REPLY = "com.yelloistaken.discordwearos.REPLY_ACTION"
const val ACTION_DISMISS = "com.yelloistaken.discordwearos.DISMISS_ACTION"
const val EXTRA_CHANNEL_ID = "channel_id"
const val EXTRA_NOTIFICATION_ID = "notification_id"
const val EXTRA_VOICE_REPLY = "voice_reply"

fun createNotificationChannels(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java)

    val messagesChannel = NotificationChannel(
        CHANNEL_ID_MESSAGES,
        "Messages",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Discord message notifications"
        enableVibration(true)
    }

    val serviceChannel = NotificationChannel(
        CHANNEL_ID_SERVICE,
        "Connection",
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = "Discord gateway connection status"
    }

    manager.createNotificationChannels(listOf(messagesChannel, serviceChannel))
}

fun buildServiceNotification(context: Context): Notification {
    val openIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat.Builder(context, CHANNEL_ID_SERVICE)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Discord")
        .setContentText("Connected")
        .setOngoing(true)
        .setContentIntent(openIntent)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()
}

fun showMessageNotification(context: Context, message: Message, notificationId: Int) {
    val replyLabel = "Reply"
    val remoteInput = RemoteInput.Builder(EXTRA_VOICE_REPLY)
        .setLabel(replyLabel)
        .build()

    val replyIntent = PendingIntent.getBroadcast(
        context,
        notificationId,
        Intent(ACTION_REPLY).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_CHANNEL_ID, message.channelId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        },
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val dismissIntent = PendingIntent.getBroadcast(
        context,
        notificationId + 10000,
        Intent(ACTION_DISMISS).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val openIntent = PendingIntent.getActivity(
        context,
        notificationId + 20000,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CHANNEL_ID, message.channelId)
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val replyAction = NotificationCompat.Action.Builder(
        android.R.drawable.ic_menu_send,
        replyLabel,
        replyIntent
    )
        .addRemoteInput(remoteInput)
        .setAllowGeneratedReplies(true)
        .build()

    val notification = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(message.author.displayName)
        .setContentText(message.content.ifBlank { "[attachment]" })
        .setContentIntent(openIntent)
        .addAction(replyAction)
        .addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Dismiss",
            dismissIntent
        )
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .extend(
            NotificationCompat.WearableExtender()
                .addAction(replyAction)
        )
        .build()

    NotificationManagerCompat.from(context).notify(notificationId, notification)
}

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        when (intent.action) {
            ACTION_DISMISS -> {
                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }
            }
            ACTION_REPLY -> {
                val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: return
                val bundle: Bundle? = RemoteInput.getResultsFromIntent(intent)
                val replyText = bundle?.getCharSequence(EXTRA_VOICE_REPLY)?.toString() ?: return

                // Cancel notification
                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }

                // Forward to MainActivity to actually send the message
                context.startActivity(
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(EXTRA_CHANNEL_ID, channelId)
                        putExtra(EXTRA_VOICE_REPLY, replyText)
                    }
                )
            }
        }
    }
}

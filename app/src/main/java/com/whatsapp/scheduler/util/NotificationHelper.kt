package com.whatsapp.scheduler.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID = "whatsapp_scheduler_channel"
    private const val CHANNEL_NAME = "WhatsApp Scheduler Notifications"
    private const val CHANNEL_DESC = "Notifications for scheduled WhatsApp message delivery"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showSendingNotification(context: Context, messageId: Long, contactName: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("WhatsApp Scheduler")
            .setContentText("Sending scheduled message to $contactName...")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)

        manager.notify(messageId.toInt(), builder.build())
    }

    fun showSuccessNotification(context: Context, messageId: Long, contactName: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentTitle("Message Sent")
            .setContentText("Message successfully sent to $contactName")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        manager.notify(messageId.toInt(), builder.build())
    }

    fun showFailureNotification(context: Context, messageId: Long, contactName: String, reason: String?) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Message Failed")
            .setContentText("Failed sending to $contactName: ${reason ?: "Unknown error"}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        manager.notify(messageId.toInt(), builder.build())
    }
}

package com.example.capstone2.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.capstone2.R

object NotificationUtils {
    private const val CHANNEL_ID = "capstone_notification_channel"
    private const val CHANNEL_NAME = "Capstone Notifications"
    private const val CHANNEL_DESCRIPTION = "Notifications for service requests"
    
    // Create the notification channel (required for Android 8.0 and above)
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    // Show a notification
    fun showNotification(
        context: Context, 
        notificationId: Int,
        title: String, 
        content: String,
        intent: Intent? = null
    ) {
        val pendingIntent = intent?.let {
            PendingIntent.getActivity(
                context, 
                0, 
                it, 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Make sure to create this drawable
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        
        pendingIntent?.let {
            builder.setContentIntent(it)
        }
        
        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                // Handle missing notification permission
            }
        }
    }
} 
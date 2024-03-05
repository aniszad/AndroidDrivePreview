package com.az.googledrivelibraryxml.utils

import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationLauncher(private val  context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationId = 321
    private val channelId = "download_channel"
    private val channelName = "Download Channel"

    private fun createNotificationBuilder(): NotificationCompat.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(context, this.channelId)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        } else {
            NotificationCompat.Builder(context, this.channelId)
                .setContentTitle("Download Notification")
                .setContentText("Download in progress...")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setLights(Color.BLUE, 1000, 1000) // Set LED light behavior (deprecated)
        }
    }

    fun startNotification(fileName: String, notificationTitle: String, ongoing: Boolean) {
        val notificationBuilder = createNotificationBuilder()
        notificationBuilder.setContentTitle(notificationTitle)
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(ongoing)
            .setProgress(0, 0, ongoing)
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun updateNotificationCompleted(fileName:String, notificationTitle: String, ongoing: Boolean) {
        notificationManager.notify(
            notificationId,
            createNotificationBuilder()
                .setContentTitle(notificationTitle)
                .setContentText(fileName)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setOngoing(ongoing)
                .build()
        )
    }

}






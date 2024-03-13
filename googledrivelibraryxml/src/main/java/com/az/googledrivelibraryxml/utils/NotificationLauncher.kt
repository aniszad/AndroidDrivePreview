package com.az.googledrivelibraryxml.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import okhttp3.internal.notify
import java.io.File

/**
 * Notification launcher
 *
 * @property context
 * @constructor Create empty Notification launcher
 */
class NotificationLauncher(private val  context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "download_channel"
    private val channelName = "Download Channel"
    private var notificationIdMap  = mutableMapOf<String, Int>()

    private val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        NotificationCompat.Builder(context, this.channelId)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    } else {
        NotificationCompat.Builder(context, this.channelId)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
    }



    /**
     * Start notification
     *
     * @param fileName
     * @param notificationTitle
     * @param ongoing
     */
    fun startNotification(fileName: String, notificationTitle: String, ongoing: Boolean) {
        val notificationId = System.currentTimeMillis().toInt()
        notificationIdMap[fileName] = notificationId
        notificationBuilder.setContentTitle(notificationTitle)
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(ongoing)
            .setProgress(0, 0, ongoing)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    /**
     * Update notification completed
     *
     * @param fileName
     * @param notificationTitle
     * @param ongoing
     */
    fun updateNotificationCompleted(fileName:String, notificationTitle: String, ongoing: Boolean) {
        notificationManager.notify(
            notificationIdMap[fileName]!!,
            notificationBuilder
                .setContentTitle(notificationTitle)
                .setContentText(fileName)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setProgress(0, 0, ongoing)
                .setOngoing(ongoing)
                .build()
        )
    }

}






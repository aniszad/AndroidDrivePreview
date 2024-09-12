package com.az.androiddrivepreview.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat

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
     * @param fileUri
     * @param mimeType
     */
    fun startNotification(fileName: String, notificationTitle: String, ongoing: Boolean, fileUri: Uri?, mimeType: String) {
        val notificationId = System.currentTimeMillis().toInt()
        notificationIdMap[fileName] = notificationId
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, mimeType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notificationBuilder.setContentTitle(notificationTitle)
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(ongoing)
            .setProgress(0, 0, ongoing)
            .setContentIntent(pendingIntent)
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        // Notify the user
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
            notificationIdMap[fileName] ?: 231,
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






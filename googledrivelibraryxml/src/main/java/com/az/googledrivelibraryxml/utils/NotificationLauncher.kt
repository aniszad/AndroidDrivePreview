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

class NotificationLauncher(private val  context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationId = 123
    private val channelId = "download_channel"
    private val channelName = "Download Channel"

    private fun createNotificationBuilder(): NotificationCompat.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(context, this.channelId)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        } else {
            NotificationCompat.Builder(context, this.channelId)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
        }

    }


    fun startNotification(fileName: String,notificationTitle: String, filePath:String,ongoing: Boolean) {
        val notificationBuilder = createNotificationBuilder()
        val file = File(filePath)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.fromFile(file), "*/*") // Replace "application/*" with appropriate mime type if known
        val pendingIntent = PendingIntent.getActivity(context.applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        notificationBuilder.setContentIntent(pendingIntent)
            .setContentTitle(notificationTitle)
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






package com.az.googledrivelibraryxml.api

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import com.az.googledrivelibraryxml.models.FileDriveItem
import com.az.googledrivelibraryxml.models.ItemType
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class GoogleDriveApi(jsonCredentialsPath : String, private val appName : String) {

    private var driveService: Drive
    private var googleCredentials: GoogleCredentials

    companion object {
        private val JSON_FACTORY: JsonFactory =
            GsonFactory.getDefaultInstance()
        val SCOPES = listOf(DriveScopes.DRIVE)
    }

    init {
        // Load client secrets from your credentials file
        val inputStream: InputStream =
            this@GoogleDriveApi::class.java.classLoader!!.getResourceAsStream(
                jsonCredentialsPath
            )
                ?: throw FileNotFoundException("Resource not found: $jsonCredentialsPath")
        inputStream.use { `in` ->
            googleCredentials = GoogleCredentials.fromStream(`in`).createScoped(
                SCOPES
            )
            driveService = Drive.Builder(
                NetHttpTransport(),
                JSON_FACTORY,
                HttpCredentialsAdapter(googleCredentials)
            )
                .setApplicationName(appName)
                .build()
        }
    }
    suspend fun getDriveFiles(folderId: String): List<FileDriveItem>? {
        return try {
            val files = withContext(Dispatchers.IO) {
                driveService.files().list()
                    .setQ("'$folderId' in parents")
                    .setFields("files(id, name, size, mimeType, webContentLink,webViewLink, createdTime, exportLinks)")
                    .execute()
            }

            val queryResultList = mutableListOf<FileDriveItem>()
            Log.e("rar", files.files.toString())
            for (file in files.files) {
                queryResultList.add(
                    FileDriveItem(
                        fileId = file.id,
                        fileName = file.name,
                        mimeType = (file.mimeType),
                        size = if (fileOrDirectory(file.mimeType) == ItemType.FILE) {
                            file.getSize()
                        } else {
                            0L
                        },
                        lastModified = file.createdTime.toString(),
                        downloadUrl = if (fileOrDirectory(file.mimeType) == ItemType.FILE) {

                            file.webContentLink
                        } else {
                            ""
                        },
                        webViewLink = if (fileOrDirectory(file.mimeType) == ItemType.FILE) {
                            file.webViewLink
                        } else {
                            ""
                        }
                    )
                )
            }
            queryResultList
        } catch (e: IOException) {
            Log.e("ERROR WHILE GETTING FILES ", e.message.toString())
            null
        }
    }
    suspend fun queryDriveFiles(folderId: String, fileNameQuery: String): List<FileDriveItem>? {
        return try {
            val files = withContext(Dispatchers.IO) {
                driveService.files().list()
                    .setQ("'$folderId' in parents and name contains '$fileNameQuery'")
                    .setFields("files(id, name, webViewLink, size, mimeType, webContentLink, createdTime)")
                    .execute()
            }
            val queryResultList = mutableListOf<FileDriveItem>()
            for (file in files.files) {
                queryResultList.add(
                    FileDriveItem(
                        fileId = file.id,
                        fileName = file.name,
                        mimeType = (file.mimeType),
                        size = file.size.toLong(),
                        lastModified = file.createdTime?.toString() ?: "",
                        downloadUrl = if (fileOrDirectory(file.mimeType) == ItemType.FILE) {
                            file.webContentLink
                        } else {
                            ""
                        },
                        webViewLink = if (fileOrDirectory(file.mimeType) == ItemType.FILE) {
                            file.webViewLink
                        } else {
                            ""
                        }
                    )
                )
            }
            queryResultList
        } catch (e: IOException) {
            Log.e("ERROR WHILE GETTING FILES ", e.message.toString())
            null
        }
    }
    suspend fun deleteFolder(folderId: String) : Boolean{
        return try{
            withContext(Dispatchers.IO){
                driveService.files().delete(folderId).execute()
            }
            true
        }catch (e : IOException){
            Log.e("ERROR WHILE DELETING FILE", e.message.toString())
            false
        }
    }
    private fun fileOrDirectory(mimeType: String): ItemType {
        return if (mimeType == "application/vnd.google-apps.folder"){
            ItemType.FOLDER
        }else{
            ItemType.FILE
        }
    }
    suspend fun createFolder(folderName: String, parentFolderId : String): String? {
        try {
            val fileMetadata = File()
            fileMetadata.name = folderName
            fileMetadata.mimeType = "application/vnd.google-apps.folder"
            fileMetadata.parents = listOf(parentFolderId)

            val createdFile = withContext(Dispatchers.IO) {
                try {
                    driveService.files().create(fileMetadata).execute()
                } catch (e: Exception) {
                    Log.e("createFolder", "Error creating folder:", e)
                    throw e // Re-throw to be handled in the main thread
                }
            }

            return createdFile.id

        } catch (e: IOException) {
            Log.e("createFolder", "Error creating folder:", e)
            // Provide user-friendly feedback based on error details
            return null
        } catch (e: SecurityException) {
            Log.e("createFolder", "Unauthorized access or insufficient permissions:", e)
            // Notify user and request necessary permissions
            return null
        } catch (e: Exception) {
            Log.e("createFolder", "Unexpected error:", e)
            // Provide generic error message and consider retry logic
            return null
        }
    }

    suspend fun downloadFileFromDrive(
        context: Context,
        fileId: String,
        fileName: String,
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = 321
        val channelId = "download_channel"
        val channelName = "Download Channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "Channel for download notifications"
            channel.enableLights(true)
            channel.lightColor = Color.BLUE
            notificationManager.createNotificationChannel(channel)
        } else {
            // For pre-Oreo versions, use deprecated NotificationCompat.Builder
            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setContentTitle("Download Notification")
                .setContentText("Download in progress...")
                .setLights(Color.BLUE, 1000, 1000) // Set LED light behavior (deprecated)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            notificationManager.notify(notificationId, notificationBuilder.build())
        }

        // Notification for download start
        val startNotification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Downloading")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .build()

        notificationManager.notify(notificationId, startNotification)

        try {
            val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
            val driveService = Drive.Builder(httpTransport, JacksonFactory(), HttpCredentialsAdapter(googleCredentials))
                .setApplicationName(appName)
                .build()

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val filePath = java.io.File(downloadsDir, fileName)

            withContext(Dispatchers.IO) {
                val outputStream: OutputStream = FileOutputStream(filePath)
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                outputStream.close()
            }
            val completionNotification = NotificationCompat.Builder(context, channelId)
                .setContentTitle("Download completed")
                .setContentText(fileName)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(notificationId, completionNotification)

        } catch (e: IOException) {
            // Handle IO exceptions
        }
    }

}
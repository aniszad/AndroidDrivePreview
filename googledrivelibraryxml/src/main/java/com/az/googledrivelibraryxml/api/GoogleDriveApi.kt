package com.az.googledrivelibraryxml.api

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import com.az.googledrivelibraryxml.exceptions.DriveApiException
import com.az.googledrivelibraryxml.managers.GdCredentialsProvider
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
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class GoogleDriveApi(gdCredentialsProvider : GdCredentialsProvider, private val appName : String) {

    private var driveService: Drive
    private var googleCredentials: GoogleCredentials

    companion object {
        private val JSON_FACTORY: JsonFactory =
            GsonFactory.getDefaultInstance()
        val SCOPES = listOf(DriveScopes.DRIVE)
    }
    init {
        // getting the input stream of the credentials file
        var credentialsInputStream = gdCredentialsProvider.getCredentials()
        credentialsInputStream.use { `in` ->
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
                        createdDate = file.createdTime.toString(),
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
            Log.e("ERROR WHILE GETTING FILES", "An error occurred while fetching files: ${e.message}", e)
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
                        createdDate = file.createdTime?.toString() ?: "",
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
            Log.e("ERROR WHILE QUERYING FILES", "An error occurred while fetching files: ${e.message}", e)
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
            Log.e("ERROR WHILE DELETING FILES", "An error occurred while fetching files: ${e.message}", e)
            false
        }catch (e : Exception){
            Log.e("ERROR WHILE DELETING FILES", "An error occurred while fetching files: ${e.message}", e)
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
        return try {
            val fileMetadata = File()
            fileMetadata.name = folderName
            fileMetadata.mimeType = "application/vnd.google-apps.folder"
            fileMetadata.parents = listOf(parentFolderId)
            val createdFile = withContext(Dispatchers.IO) {
                    driveService.files().create(fileMetadata).execute()
            }
            createdFile?.id
        } catch (e: IOException) {
            Log.e("ERROR WHILE FILE CREATION", "An error occurred while fetching files: ${e.message}", e)
            null
        } catch (e: SecurityException) {
            Log.e("ERROR WHILE FILE CREATION", "An error occurred while fetching files: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e("ERROR WHILE FILE CREATION", "An error occurred while fetching files: ${e.message}", e)
            null
        }
    }
    suspend fun downloadFileFromDrive(
        context: Context,
        fileId: String,
        fileName: String,
    ) {
        try {
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

            // initiating download
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
            Log.e("ERROR WHILE FILE CREATION", "An error occurred while fetching files: ${e.message}", e)
        } catch (e: SecurityException) {
            Log.e("ERROR WHILE FILE CREATION", "An error occurred while fetching files: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("ERROR WHILE FILE CREATION", "An error occurred while fetching files: ${e.message}", e)
        }
    }


}
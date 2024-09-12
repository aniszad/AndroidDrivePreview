package com.az.androiddrivepreview.api

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.az.androiddrivepreview.data.exceptions.DriveDownloadException
import com.az.androiddrivepreview.data.interfaces.GdCredentialsProvider
import com.az.androiddrivepreview.data.models.FileDriveItem
import com.az.androiddrivepreview.data.models.ItemType
import com.az.androiddrivepreview.utils.NotificationLauncher
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URLConnection


/**
 * Google drive api
 *
 * @property notificationLauncher
 * @property appName
 * @constructor
 *
 * @param gdCredentialsProvider
 */
class GoogleDriveApi(
    private val context: Context,
    gdCredentialsProvider: GdCredentialsProvider,
    private val notificationLauncher: NotificationLauncher,
    private val appName: String
) {

    private var driveService: Drive
    private var googleCredentials: GoogleCredentials
    private var downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.toUri()
        ?: context.filesDir.toUri()


    companion object {
        private val JSON_FACTORY: JsonFactory =
            GsonFactory.getDefaultInstance()
        val SCOPES = listOf(DriveScopes.DRIVE)
    }

    init {
        // getting the input stream of the credentials file
        val credentialsInputStream = gdCredentialsProvider.getCredentials()
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

    /**
     * Get drive files
     *
     * @param folderId
     * @return
     */
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
            Log.e(
                "ERROR WHILE GETTING FILES",
                "An error occurred while fetching files: ${e.message}",
                e
            )
            null
        }
    }

    /**
     * Query drive files
     *
     * @param folderId
     * @param fileNameQuery
     * @return
     */
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
                        size = if (fileOrDirectory(file.mimeType) == ItemType.FILE) {
                            file.getSize()
                        } else {
                            0L
                        },
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
            Log.e(
                "ERROR WHILE QUERYING FILES",
                "An error occurred while fetching files: ${e.message}",
                e
            )
            null
        }
    }

    /**
     * Delete folder
     *
     * @param folderId
     * @return
     */
    suspend fun deleteFolder(folderId: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                driveService.files().delete(folderId).execute()
            }
            true
        } catch (e: IOException) {
            Log.e(
                "ERROR WHILE DELETING FILES",
                "An error occurred while fetching files: ${e.message}",
                e
            )
            false
        } catch (e: Exception) {
            Log.e(
                "ERROR WHILE DELETING FILES",
                "An error occurred while fetching files: ${e.message}",
                e
            )
            false
        }
    }

    /**
     * Determines whether a file is a folder or a file based on its MIME type.
     *
     * Determines whether a file is a folder or a file based on its MIME type.
     * Returns ItemType.FOLDER if the MIME type indicates a folder, ItemType.FILE otherwise.
     *
     * @param mimeType The MIME type of the file.
     * @return ItemType.FOLDER if the MIME type indicates a folder, ItemType.FILE otherwise.
     */
    private fun fileOrDirectory(mimeType: String): ItemType {
        return if (mimeType == "application/vnd.google-apps.folder") {
            ItemType.FOLDER
        } else {
            ItemType.FILE
        }
    }

    /**
     * Create folder
     *
     * @param folderName
     * @param parentFolderId
     * @return
     */
    suspend fun createFolder(folderName: String, parentFolderId: String): String? {
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
            Log.e(
                "ERROR WHILE FILE CREATION",
                "An error occurred while fetching files: ${e.message}",
                e
            )
            null
        } catch (e: SecurityException) {
            Log.e(
                "ERROR WHILE FILE CREATION",
                "An error occurred while fetching files: ${e.message}",
                e
            )
            null
        } catch (e: Exception) {
            Log.e(
                "ERROR WHILE FILE CREATION",
                "An error occurred while fetching files: ${e.message}",
                e
            )
            null
        }
    }

    /**
     * Download file from drive
     * only uses uri provided by SAF
     *
     * @param fileId
     * @param fileName
     */
    suspend fun downloadFileFromDrive(
        fileId: String,
        fileName: String,
        currentNamesPath: MutableList<String>
    ) {
        withContext(Dispatchers.IO) {
            try {
                val mimeType = URLConnection.guessContentTypeFromName(fileName)
                    ?: "*/*"  // Fallback to wildcard
                var parentDocument = DocumentFile.fromTreeUri(context, downloadsDir)

                if (parentDocument == null || !parentDocument.exists()) {
                    Log.e("SAF", "Root directory does not exist!")
                    return@withContext
                }
                for (folder in currentNamesPath) {
                    val existingDir = parentDocument?.findFile(folder)
                    parentDocument = existingDir ?: parentDocument?.createDirectory(folder)
                }
                val targetFile = parentDocument?.findFile(fileName)
                    ?: parentDocument?.createFile(
                        mimeType,
                        fileName
                    )  // Use dynamically detected MIME type

                notificationLauncher.startNotification(
                    fileName,
                    "Download started",
                    true,
                    targetFile?.uri,
                    mimeType
                )

                if (targetFile != null) {
                    context.contentResolver.openOutputStream(targetFile.uri)?.use { outputStream ->
                        driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                    }

                    notificationLauncher.updateNotificationCompleted(
                        fileName,
                        "Download completed",
                        false
                    )
                } else {
                    throw Exception("Failed to create file: $fileName")
                }
            } catch (e: Exception) {
                notificationLauncher.updateNotificationCompleted(fileName, "Download failed", false)
                throw DriveDownloadException(e.stackTraceToString())
            }
        }
    }


    /**
     * Create file on drive
     *
     * @param javaFile
     * @param parentId
     * @param mimeType
     */
    suspend fun createFileOnDrive(javaFile: java.io.File, parentId: String, mimeType: String) {
        val fileMetadata = File()
        fileMetadata.name = javaFile.name
        fileMetadata.parents = listOf(parentId)

        val mediaContent = FileContent(mimeType, javaFile)


        try {
            withContext(Dispatchers.IO) {
                val fileCreation = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
                println("File ID: ${fileCreation.id}")
            }

        } catch (e: Exception) {
            println("Error creating file: $e")
        }
    }

    /**
     * Set download path
     *
     * it's sets a new download folder
     *
     * @param downloadPath
     */
    fun setDownloadPath(downloadPath: Uri) {
        this.downloadsDir = downloadPath
    }


}
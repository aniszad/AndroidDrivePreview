package com.az.googledrivelibraryxml.api

import android.os.Environment
import android.util.Log
import com.az.googledrivelibraryxml.exceptions.DriveDownloadException
import com.az.googledrivelibraryxml.managers.GdCredentialsProvider
import com.az.googledrivelibraryxml.models.FileDriveItem
import com.az.googledrivelibraryxml.models.ItemType
import com.az.googledrivelibraryxml.utils.NotificationLauncher
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
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


class GoogleDriveApi(gdCredentialsProvider : GdCredentialsProvider,
                     private val notificationLauncher: NotificationLauncher,
                     private val appName : String) {

    private var driveService: Drive
    private var googleCredentials: GoogleCredentials

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
     * Retrieves files from Google Drive for the specified folder.
     *
     * Retrieves files from Google Drive for the specified folder ID.
     * Returns a list of FileDriveItem representing the files in the folder.
     * Handles IOException and returns null if an error occurs during retrieval.
     *
     * @param folderId The ID of the folder from which files are to be retrieved.
     * @return A list of FileDriveItem representing the files in the folder, or null if an error occurs.
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
            Log.e("ERROR WHILE GETTING FILES", "An error occurred while fetching files: ${e.message}", e)
            null
        }
    }

    /**
     * Queries files in Google Drive based on the provided query string.
     *
     * Queries files in Google Drive based on the provided query string and folder ID.
     * Returns a list of FileDriveItem representing the queried files.
     * Handles IOException and returns null if an error occurs during querying.
     *
     * @param folderId The ID of the folder in which to search for files.
     * @param fileNameQuery The query string used to search for files by name.
     * @return A list of FileDriveItem representing the queried files, or null if an error occurs.
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

    /**
     * Deletes a folder from Google Drive.
     *
     * Deletes a folder from Google Drive based on the provided folder ID.
     * Returns true if the folder is deleted successfully, false otherwise.
     * Handles IOException and returns false if an error occurs during deletion.
     *
     * @param folderId The ID of the folder to be deleted.
     * @return True if the folder is deleted successfully, false otherwise.
     */
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
        return if (mimeType == "application/vnd.google-apps.folder"){
            ItemType.FOLDER
        }else{
            ItemType.FILE
        }
    }

    /**
     * Creates a new folder in Google Drive.
     *
     * Creates a new folder with the provided folder name within the specified parent folder.
     * Returns the ID of the newly created folder if successful, null otherwise.
     * Handles IOException and returns null if an error occurs during folder creation.
     *
     * @param folderName The name of the folder to be created.
     * @param parentFolderId The ID of the parent folder in which to create the new folder.
     * @return The ID of the newly created folder if successful, null otherwise.
     */
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

    /**
     * Downloads a file from Google Drive.
     *
     * Downloads a file from Google Drive with the provided file ID and file name.
     * Saves the downloaded file to the device's Downloads directory.
     * Throws a DriveDownloadException if an error occurs during download.
     *
     * @param fileId The ID of the file to be downloaded.
     * @param fileName The name to be used for the downloaded file.
     */
    suspend fun downloadFileFromDrive(
        fileId: String,
        fileName: String,
    ) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val filePath = java.io.File(downloadsDir, fileName)
        Log.e(" filepath", filePath.path)
        withContext(Dispatchers.IO) {
            try {
                val outputStream: OutputStream = FileOutputStream(filePath)
                notificationLauncher.startNotification(fileName, "Download started", filePath.path,true)
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                outputStream.close()
            }catch (e : Exception){
                throw DriveDownloadException(e.stackTraceToString())
            }

        }
        notificationLauncher.updateNotificationCompleted(fileName, "Download completed", false)

    }




    /**
     * Creates a file on Google Drive.
     *
     * Creates a file on Google Drive with the provided Java File, parent folder ID, and MIME type.
     * Uses the file name from the Java File and sets the parent folder ID.
     * Uses the MIME type to create the appropriate media content for the file.
     * Executes the file creation request and prints the file ID if successful.
     * Handles exceptions and prints error messages if file creation fails.
     *
     * @param javaFile The Java File to be uploaded to Google Drive.
     * @param parentId The ID of the parent folder in which to create the file.
     * @param mimeType The MIME type of the file.
     */
    suspend fun createFileOnDrive(javaFile: java.io.File, parentId: String, mimeType: String) {
        val fileMetadata = File()
        fileMetadata.name = javaFile.name
        fileMetadata.parents = listOf(parentId)

        val mediaContent = FileContent(mimeType, javaFile)


        try {
            withContext(Dispatchers.IO) {
                val fileCreation= driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
                println("File ID: ${fileCreation.id}")
            }

        } catch (e: Exception) {
            println("Error creating file: ${e}")
        }
    }


}
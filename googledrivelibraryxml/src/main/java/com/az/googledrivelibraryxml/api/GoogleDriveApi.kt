package com.az.googledrivelibraryxml.api

import android.util.Log
import com.az.googledrivelibraryxml.models.FileDriveItem
import com.az.googledrivelibraryxml.models.ItemType
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class GoogleDriveApi(private val jsonCredentialsPath : String, private val appName : String) {

    private var driveService: Drive
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private val JSON_FACTORY: JsonFactory =
            GsonFactory.getDefaultInstance()
        private val SCOPES = listOf(DriveScopes.DRIVE)
    }

    init {
        // Load client secrets from your credentials file
        val `in`: InputStream =
            this@GoogleDriveApi::class.java.classLoader!!.getResourceAsStream(
                jsonCredentialsPath
            )
                ?: throw FileNotFoundException("Resource not found: $jsonCredentialsPath")
        val googleCredentials: GoogleCredentials = GoogleCredentials.fromStream(`in`).createScoped(
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

    suspend fun getDriveFiles(folderId: String): List<FileDriveItem>? {
        return try {
            val files = withContext(Dispatchers.IO) {
                driveService.files().list()
                    .setQ("'$folderId' in parents")
                    .setFields("files(id, name, webViewLink, size, mimeType, webContentLink, webViewLink, modifiedTime)")
                    .execute()
            }
            val results = files
            val queryResultList = mutableListOf<FileDriveItem>()
            for (file in results.files) {
                queryResultList.add(
                    FileDriveItem(
                        fileId = file.id,
                        fileName = file.name,
                        mimeType = (file.mimeType),
                        size = if (fileOrDirectory(file.mimeType) == ItemType.FILE){
                            file.getSize()
                        }else{
                            0L
                        },
                        lastModified = file.modifiedTime.toString(),
                        downloadUrl = if (fileOrDirectory(file.mimeType) == ItemType.FILE){

                            file.webContentLink
                        }else{
                            ""
                        },
                        webViewLink = if (fileOrDirectory(file.mimeType) == ItemType.FILE){
                            file.webViewLink
                        }else{
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
            val files = scope.async {
                driveService.files().list()
                    .setQ("'$folderId' in parents and name contains '$fileNameQuery'")
                    .setFields("files(id, name, webViewLink, size, mimeType, webContentLink, exportLinks)")
                    .execute()
            }
            val results = files.await()
            val queryResultList = mutableListOf<FileDriveItem>()
            for (file in results.files) {
                queryResultList.add(
                    FileDriveItem(
                        fileId = file.id,
                        fileName = file.name,
                        mimeType = (file.mimeType),
                        size = file.size.toLong(),
                        lastModified = file.modifiedTime?.toString() ?: "",
                        downloadUrl = if (fileOrDirectory(file.mimeType) == ItemType.FILE) {
                            file.webContentLink
                        } else {
                            ""
                        },
                        webViewLink = if (fileOrDirectory(file.mimeType) == ItemType.FILE){
                            file.webViewLink
                        }else{
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
    suspend fun createFolderInPath(folderName: String, parentFolderIds: List<String>): String? {
        return try {
            // Create the metadata for the new folder
            val folderMetadata = File()
                .setName(folderName)
                .setMimeType("application/vnd.google-apps.folder")
                .setParents(parentFolderIds)

            // Create the folder using the Drive API
            val scope = CoroutineScope(Dispatchers.IO)
            val folder = scope.async {
                driveService.files()
                    .create(folderMetadata).execute()
            }.await()

            // Return the ID of the newly created folder
            folder?.id
        } catch (e: IOException) {
            Log.e("ERROR WHILE CREATING FOLDER ", e.message.toString())
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
            Log.e("hehehehhe", e.toString())
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

            val createdFile = scope.async(Dispatchers.IO) {
                try {
                    driveService.files().create(fileMetadata).execute()
                } catch (e: Exception) {
                    Log.e("createFolder", "Error creating folder:", e)
                    throw e // Re-throw to be handled in the main thread
                }
            }.await()

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



}
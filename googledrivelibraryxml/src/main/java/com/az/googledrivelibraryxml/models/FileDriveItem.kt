package com.az.googledrivelibraryxml.models

data class FileDriveItem(
    val fileId: String,
    val fileName: String,
    val mimeType: String,
    val size : Long,
    val lastModified : String,
    val downloadUrl : String,
    val webViewLink : String,
) : FileSystemItem(fileName, "", mimeType, size)


package com.az.googledrivelibraryxml.models

data class FileDriveItem(
    val fileId: String,
    val fileName: String,
    val fileType: ItemType,
    val size : Long,
    val lastModified : String,
    val downloadUrl : String,
    val webViewLink : String,
) : FileSystemItem(fileName, "", fileType, size)


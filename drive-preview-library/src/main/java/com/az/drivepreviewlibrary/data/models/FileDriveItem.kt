package com.az.drivepreviewlibrary.data.models

data class FileDriveItem(
    val fileId: String,
    val fileName: String,
    val mimeType: String,
    val size : Long,
    val createdDate : String,
    val downloadUrl : String,
    val webViewLink : String,
)


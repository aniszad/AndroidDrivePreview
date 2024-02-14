package com.az.googledrivelibraryxml.models

import java.sql.Timestamp

open class FileSystemItem(
    val name: String,
    val path: String,
    val type: String,
    val fileSize : Long,
    var timestamp: Timestamp = Timestamp(0L),
)


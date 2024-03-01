package com.az.googledrivelibraryxml.utils

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.az.googledrivelibraryxml.R
import com.az.googledrivelibraryxml.models.ItemType
import kotlin.math.pow

class FileDetailsAdapter {


    // return an icon as a drawable basing on the mimetype
    fun getIconFromMimeType(context: Context, mimeType: String): Drawable? {
        if (fileOrDirectory(mimeType) == ItemType.FOLDER) {
            return ContextCompat.getDrawable(context, R.drawable.icon_folder)
        }

        return when {
            mimeType.startsWith("image/") -> {
                when (mimeType) {
                    "image/gif" -> ContextCompat.getDrawable(context, R.drawable.icon_gif)
                    "image/jpeg" -> ContextCompat.getDrawable(context, R.drawable.icon_jpg)
                    "image/png" -> ContextCompat.getDrawable(context, R.drawable.icon_png)
                    "image/svg+xml" -> ContextCompat.getDrawable(context, R.drawable.icon_svg)
                    else -> ContextCompat.getDrawable(context, R.drawable.icon_img) // For other image types
                }
            }
            mimeType == "application/pdf" -> ContextCompat.getDrawable(context, R.drawable.icon_pdf)
            mimeType == "audio/mpeg" -> ContextCompat.getDrawable(context, R.drawable.icon_mp3)
            mimeType == "video/x-msvideo" -> ContextCompat.getDrawable(context, R.drawable.icon_avi)
            mimeType == "video/x-matroska" -> ContextCompat.getDrawable(context, R.drawable.icon_mkv)
            mimeType == "application/zip" -> ContextCompat.getDrawable(context, R.drawable.icon_zip)
            mimeType == "image/vnd.adobe.photoshop" -> ContextCompat.getDrawable(context, R.drawable.icon_psd)
            mimeType == "text/plain" -> ContextCompat.getDrawable(context, R.drawable.icon_txt)
            mimeType == "application/illustrator" -> ContextCompat.getDrawable(context, R.drawable.icon_ai)
            mimeType == "application/msword" -> ContextCompat.getDrawable(context, R.drawable.icon_doc)
            mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ContextCompat.getDrawable(context, R.drawable.icon_doc)
            mimeType == "application/vnd.ms-powerpoint" -> ContextCompat.getDrawable(context, R.drawable.icon_ppt)
            mimeType == "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> ContextCompat.getDrawable(context, R.drawable.icon_ppt)
            mimeType == "application/vnd.ms-excel" -> ContextCompat.getDrawable(context, R.drawable.icon_xls)
            mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ContextCompat.getDrawable(context, R.drawable.icon_xls)
            mimeType == "application/json" -> ContextCompat.getDrawable(context, R.drawable.icon_json)
            mimeType == "text/csv" -> ContextCompat.getDrawable(context, R.drawable.icon_csv)
            mimeType == "application/x-rar-compressed" -> ContextCompat.getDrawable(context, R.drawable.icon_rar) // Adding RAR icon
            mimeType == "application/x-zip-compressed" -> ContextCompat.getDrawable(context, R.drawable.icon_zip) // Adding RAR icon
            else -> ContextCompat.getDrawable(context, R.drawable.icon_other) // Replace with a default icon
        }
    }

    //check if the file is a folder or a file
    fun fileOrDirectory(mimeType: String): ItemType {
        return if (mimeType == "application/vnd.google-apps.folder"){
            ItemType.FOLDER
        }else{
            ItemType.FILE
        }
    }

    // format the file size to a presentable text
    fun formatSize(size: Long): String {
        return when{
            size == 0L ->{""}

            size/1000.0.pow(3.0) >= 1000 ->{
                "${size/ 1000.0.pow(3.0)} GB"
            }
            (size/1000.0.pow(2.0) >= 1000) ->{
                "${size/1000.0.pow(2.0)} MB"
            }
            size/1000 < 1000 ->{
                "${size/1000} KB"
            }
            else ->{""}
        }
    }

}
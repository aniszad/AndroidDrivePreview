package com.az.androiddrivepreview

import android.content.Context
import android.content.SharedPreferences

class SharedPref(private val context: Context) {

    companion object {
        private const val PREF_NAME = "download_prefs"
        private const val KEY_DOWNLOAD_FOLDER_ACCESS_GRANTED = "download_folder_access_granted"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    var isDownloadFolderAccessGranted: Boolean
        get() = sharedPreferences.getBoolean(KEY_DOWNLOAD_FOLDER_ACCESS_GRANTED, false)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_DOWNLOAD_FOLDER_ACCESS_GRANTED, value).apply()
        }
}

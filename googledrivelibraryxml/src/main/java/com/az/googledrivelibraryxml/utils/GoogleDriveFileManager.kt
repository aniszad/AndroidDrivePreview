package com.az.googledrivelibraryxml.utils

import android.content.Context
import android.content.Intent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.az.googledrivelibraryxml.adapters.GdFilesAdapter
import com.az.googledrivelibraryxml.api.GoogleDriveApi
import com.az.googledrivelibraryxml.models.FileDriveItem
import com.az.googledrivelibraryxml.models.Permissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class GoogleDriveFileManager(
    private val context: Context,
    private val jsonCredentialsPath: String,
    private val applicationName: String,
    private val rootFileId : String
) : GdFilesAdapter.FileOptions {

    private lateinit var adapter: GdFilesAdapter
    private var googleDriveApi: GoogleDriveApi =
        GoogleDriveApi(jsonCredentialsPath = jsonCredentialsPath, appName = applicationName)
    private val scope = CoroutineScope(Dispatchers.IO)

    private var openFileByIntent = true

    // paths
    private val currentPath = rootFileId


    init {
        getFiles(rootFileId)
    }

    private fun getFiles(rootFileId: String){
        try {
            scope.launch {
                val files = googleDriveApi.getDriveFiles(rootFileId)
                scope.launch(Dispatchers.Main) {
                    updateRecyclerView(files)
                }
            }

        } catch (e: IOException) {
            // Handle the exception if needed
        }catch (e: Exception) {
            // Handle the exception if needed
        }
    }

    private fun updateRecyclerView(files: List<FileDriveItem>?) {
        if (files!=null) adapter.updateData(files)

    }

    fun setRecyclerView(recyclerView: RecyclerView) {
        adapter = GdFilesAdapter(context, listOf(), listOf(Permissions.ADMIN))
        adapter.setFileOptionsInterface(this@GoogleDriveFileManager)
        recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter
    }


    override fun onDownload(fileId: String) {

    }

    override fun onShare(webViewLink: String) {
        shareFile(webViewLink)
    }

    override fun onDelete(fileId: String) {
        TODO("Not yet implemented")
    }

    private fun shareFile(link: String) {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Share File Link")
        sharingIntent.putExtra(Intent.EXTRA_TEXT, link)
        context.startActivity(Intent.createChooser(sharingIntent, "Share File Link"))
    }


}
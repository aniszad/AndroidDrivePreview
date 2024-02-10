package com.az.googledrivelibraryxml.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.widget.Toolbar
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.az.googledrivelibraryxml.R
import com.az.googledrivelibraryxml.adapters.GdFilesAdapter
import com.az.googledrivelibraryxml.adapters.GdFilesAdapter.AccessFileListener
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
) : GdFilesAdapter.FileOptions, GdFilesAdapter.AccessFileListener {

    private lateinit var adapter: GdFilesAdapter
    private var googleDriveApi: GoogleDriveApi =
        GoogleDriveApi(jsonCredentialsPath = jsonCredentialsPath, appName = applicationName)
    private val scope = CoroutineScope(Dispatchers.IO)

    private var openFileByIntent = true

    // paths
    private val currentPath = rootFileId
    private lateinit var toolbar: Toolbar


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

    override fun onDownload(fileId: String) {

    }

    override fun onShare(webViewLink: String) {
        shareFile(webViewLink)
    }

    override fun onDelete(fileId: String) {
        scope.launch(Dispatchers.IO) {
            Log.e("hehehehhe", fileId)
            val isDeleted = googleDriveApi.deleteFolder(fileId)
            Log.e("hehehehhe", isDeleted.toString())
            getFiles(currentPath)
        }
    }

    private fun shareFile(link: String) {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Share File Link")
        sharingIntent.putExtra(Intent.EXTRA_TEXT, link)
        context.startActivity(Intent.createChooser(sharingIntent, "Share File Link"))
    }
    override fun onOpen(webContentLink: String) {
        openFileFromDrive(webContentLink)
    }





    private fun openFileFromDrive(webContentLink: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(webContentLink)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Handle case where no suitable activity is found to handle the intent
            // For example, show a toast or alert dialog indicating that no suitable app is installed
        }
    }


    // User Input ------------------------------------------------------------
    fun setRecyclerView(recyclerView: RecyclerView) {
        adapter = GdFilesAdapter(context, listOf(), listOf(Permissions.ADMIN))
        adapter.setFileOptionsInterface(this@GoogleDriveFileManager)
        adapter.setAccessFileListener(this@GoogleDriveFileManager)
        recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter
    }
    fun setAccessFileListener(accessFileListener: AccessFileListener){
        adapter.setAccessFileListener(accessFileListener)
    }

    fun setActionBar(toolbar: Toolbar) {
            // Inflate the menu XML file into the Toolbar
        toolbar.inflateMenu(R.menu.toolbar_menu)
        toolbar.title = "Something"

            // Optionally, handle menu item clicks
            toolbar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.btn_create_folder -> {
                        // Handle action_item1 click
                        true
                    }
                    R.id.btn_search -> {
                        // Handle action_item2 click
                        true
                    }
                    else -> false
                }
            }
    }

}
package com.az.googledrivelibraryxml.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Environment
import android.view.Menu
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.az.googledrivelibraryxml.R
import com.az.googledrivelibraryxml.adapters.GdFilesAdapter
import com.az.googledrivelibraryxml.adapters.GdFilesAdapter.AccessFileListener
import com.az.googledrivelibraryxml.adapters.GdFilesAdapter.AccessFolderListener
import com.az.googledrivelibraryxml.adapters.GdFilesAdapter.FileOptions
import com.az.googledrivelibraryxml.api.GoogleDriveApi
import com.az.googledrivelibraryxml.models.FileDriveItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class GoogleDriveFileManager(
    private val context: Context,
    private val rootFileId : String,
    private val lifecycleCoroutineScope: LifecycleCoroutineScope,
    private val permissions : Permissions,
    jsonCredentialsPath: String,
    applicationName: String,
) : FileOptions, AccessFileListener, AccessFolderListener {

    private lateinit var adapter: GdFilesAdapter
    private var googleDriveApi: GoogleDriveApi =
        GoogleDriveApi(jsonCredentialsPath = jsonCredentialsPath, appName = applicationName)
    private lateinit var pathTextView : TextView
    // paths
    private val currentIdsPath = mutableListOf<String>()
    private var currentNamesPath = mutableListOf("")
    private lateinit var toolbar: Toolbar
    private val createFolderDialog =  CreateFileDialog(context)

    init {
        currentIdsPath.add(rootFileId)
    }

    // API functions -------------------------------------------------------------------------------
    fun getFiles(rootFileId: String){
        adapter.showLoading()
        lifecycleCoroutineScope.launch {
            val files = googleDriveApi.getDriveFiles(rootFileId)
            withContext(Dispatchers.Main) {
                updateRecyclerView(files)
            }
        }
    }
    fun queryFiles(query: String) {
        adapter.showLoading()
        lifecycleCoroutineScope.launch {
            val files = googleDriveApi.queryDriveFiles(rootFileId, query)
            withContext(Dispatchers.Main) {
                updateRecyclerView(files)
            }
        }
    }
    private fun createFolder(folderName: String, parentFolderId : String) {
        lifecycleCoroutineScope.launch {
            val createdFolderId = googleDriveApi.createFolder(folderName, parentFolderId)
            if (createdFolderId != null){
                withContext(Dispatchers.Main) {
                    createFolderDialog.hideCreateFolderDialog()
                }
            }
        }
    }

    // Adapter interfaces implementation -----------------------------------------------------------
    override fun onDownload(fileId: String, fileName : String) {
        lifecycleCoroutineScope.launch {
            googleDriveApi.downloadFileFromDrive(context, fileId, fileName)
        }
    }
    override fun onShare(webViewLink: String) {
        shareFile(webViewLink)
    }
    override fun onDelete(fileId: String) {
        lifecycleCoroutineScope.launch(Dispatchers.IO) {
            val isDeleted = googleDriveApi.deleteFolder(fileId)
            if (isDeleted){
                withContext(Dispatchers.Main){
                    getFiles(currentIdsPath.last())
                }
            }
        }
    }
    override fun onOpenFile(webContentLink: String) {
        openFileInDriveApp(webContentLink)
    }
    override fun onOpenFolder(folderId: String, folderName : String) {
        navigateForward(folderId, folderName)
    }
    // Navigation functions ------------------------------------------------------------------------
    fun navigateBack(){
        currentIdsPath.remove(currentIdsPath.last())
        currentNamesPath.remove(currentNamesPath.last())
        setPathView(currentNamesPath.last(), currentIdsPath.last())
        getFiles(currentIdsPath.last())
    }
    private fun navigateForward(folderId: String, folderName : String){
        currentIdsPath.add(folderId)
        currentNamesPath.add(folderName)
        setPathView(folderName, folderId)
        getFiles(folderId)
    }

    // setting the path text ("root/folder1/folder2") if the view is provided
    private fun setPathView(folderName: String, folderId:String) {
        if (::pathTextView.isInitialized){
            var formattedPath = ""
            for (name in currentNamesPath){
                formattedPath += buildString {
                    append("/")
                    append(name)
                }
            }
            this.pathTextView.text = formattedPath
        }
        if (folderId == this.rootFileId){
            toolbar.navigationIcon = null
        }else{
            toolbar.navigationIcon = ContextCompat.getDrawable(context, R.drawable.icon_arrow_left)
        }
        toolbar.title = folderName
    }
    private fun openFileInDriveApp(contentViewLink: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(contentViewLink)
        intent.setPackage("com.google.android.apps.docs") // Specify the package name of Google Drive app
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    // User Input ----------------------------------------------------------------------------------
    fun setRecyclerView(recyclerView: RecyclerView): GoogleDriveFileManager {
        adapter = GdFilesAdapter(context, emptyList(), listOf(permissions))
        adapter.setFileOptionsInterface(this@GoogleDriveFileManager)
        adapter.setAccessFileListener(this@GoogleDriveFileManager)
        adapter.setAccessFolderListener(this@GoogleDriveFileManager)
        recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter
        getFiles(rootFileId)
        return this@GoogleDriveFileManager
    }
    fun setPathTextView(textView: TextView): GoogleDriveFileManager {
        this.pathTextView = textView
        this.pathTextView.text = currentNamesPath.first()
        return this@GoogleDriveFileManager
    }
    fun setAccessFileListener(accessFileListener: AccessFileListener): GoogleDriveFileManager {
        adapter.setAccessFileListener(accessFileListener)
        return this@GoogleDriveFileManager
    }
    fun setActionBar(toolbar: Toolbar): GoogleDriveFileManager {
        // Inflate the menu resource
        this.toolbar = toolbar
        when (this.permissions) {
            Permissions.ADMIN -> {
                toolbar.inflateMenu(R.menu.admin_toolbar_menu)
            }
            Permissions.USER->{
                toolbar.inflateMenu(R.menu.strict_toolbar_menu)
            }
            Permissions.STRICT->{
                toolbar.inflateMenu(R.menu.strict_toolbar_menu)
            }
        }
        toolbar.setNavigationOnClickListener {
            navigateBack()
        }
        toolbar.title = currentNamesPath.first()
        setSearchViewStyle(toolbar.menu)
        setSearchViewFunctionality(toolbar.menu)

        // Set menu item click listener
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.btn_create_folder -> {
                    showFileCreateDialog()
                    true
                }
                R.id.btn_search -> {
                    // Handle menu item 2 click
                    true
                }
                else -> false
            }
        }
        return this@GoogleDriveFileManager
    }

    // Dialogs -------------------------------------------------------------------------------------
    private fun showFileCreateDialog() {
        createFolderDialog.showCreateFolderDialog { folderName ->
            createFolder(folderName, currentIdsPath.last())
        }
    }

    // UI updates ----------------------------------------------------------------------------------
    private fun updateRecyclerView(files: List<FileDriveItem>?) {
        if (files!=null){
            adapter.updateData(files)
            adapter.hideLoading()
        }

    }

    // Style customization -------------------------------------------------------------------------
    private fun setSearchViewStyle(menu: Menu) {
        val searchItem = menu.findItem(R.id.btn_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = buildString { append("search") }
        val search = searchItem.actionView as SearchView
        val editText = search.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        editText.setTextColor(ContextCompat.getColor(context, R.color.black))
        editText.setHintTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        val closeButton = search.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeButton.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.black))
        val searchButton = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_button)
        searchButton.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.black))
    }

    // other ---------------------------------------------------------------------------------------
    private fun shareFile(link: String) {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Share File Link")
        sharingIntent.putExtra(Intent.EXTRA_TEXT, link)
        context.startActivity(Intent.createChooser(sharingIntent, "Share File Link"))
    }

    /*private fun downloadPublicFile(fileUrl: String, fileName: String) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(fileUrl)

        val request = DownloadManager.Request(downloadUri)
            .setTitle(fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        downloadManager.enqueue(request)
    }

     */

    fun setRootFileName(rootFileName: String) {
        this.currentNamesPath[0] = rootFileName
        this.pathTextView.text = buildString {
            append("$rootFileName/")
        }
        this.toolbar.title = rootFileName
    }

    private fun setSearchViewFunctionality(menu: Menu?) {
        val searchItem = menu?.findItem(R.id.btn_search)
        val searchView = searchItem?.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }
            override fun onQueryTextChange(query: String?): Boolean {
                if (query != null){
                    queryFiles(query)
                }
                return true
            }
        })
    }
}
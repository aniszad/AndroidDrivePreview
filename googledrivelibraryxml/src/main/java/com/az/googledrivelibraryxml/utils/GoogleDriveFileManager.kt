package com.az.googledrivelibraryxml.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.util.Log
import android.view.Menu
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.az.googledrivelibraryxml.R
import com.az.googledrivelibraryxml.adapters.GdFilesAdapter
import com.az.googledrivelibraryxml.adapters.GdFilesAdapter.AccessFileListener
import com.az.googledrivelibraryxml.adapters.GdFilesAdapter.AccessFolderListener
import com.az.googledrivelibraryxml.adapters.GdFilesAdapter.FileOptions
import com.az.googledrivelibraryxml.api.GoogleDriveApi
import com.az.googledrivelibraryxml.exceptions.DriveRootException
import com.az.googledrivelibraryxml.exceptions.DriveUiElementsMissing
import com.az.googledrivelibraryxml.managers.GdCredentialsProvider
import com.az.googledrivelibraryxml.models.FileDriveItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.userAgent


class GoogleDriveFileManager(
    private val context: Context,
    private val lifecycleCoroutineScope: LifecycleCoroutineScope,
    private val permissions : Permissions,
    gdCredentialsProvider: GdCredentialsProvider,
    applicationName: String,
) : FileOptions, AccessFileListener, AccessFolderListener {

    private lateinit var adapter: GdFilesAdapter
    private var googleDriveApi: GoogleDriveApi =
        GoogleDriveApi(gdCredentialsProvider = gdCredentialsProvider, appName = applicationName)
    private var clipboardManager: ClipboardManager
    private val currentIdsPath = mutableListOf<String>()
    private var currentNamesPath = mutableListOf("Drive Folder")
    private lateinit var toolbar: Toolbar
    private val createFolderDialog =  CreateFileDialog(context)
    private lateinit var swipeRefreshLayout :   SwipeRefreshLayout
    private lateinit var recyclerView : RecyclerView
    private lateinit var rootFolderId : String
    private var filePathCopyable = false
    private var useNavigationPath = false


    init {
        val clipboardServiceClass = Class.forName("android.content.ClipboardManager")
        clipboardManager = getSystemService(context, clipboardServiceClass) as ClipboardManager
    }


    // API calling functions ///////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private fun getFiles(folderName: String, folderId: String){
        adapter.showLoading()
        lifecycleCoroutineScope.launch {
            val files = googleDriveApi.getDriveFiles(folderId)
            if (files!=null){
                withContext(Dispatchers.Main) {
                    updateRecyclerView(orderByFoldersFirst(files))
                    updateToolbar(folderName, folderId)
                    updateTextPathView()
                }
            }
        }
    }
    private fun queryFiles(query: String) {
        adapter.showLoading()
        lifecycleCoroutineScope.launch {
            val files = googleDriveApi.queryDriveFiles(currentIdsPath.last(), query)
            if (files!=null){
                withContext(Dispatchers.Main) {
                    updateRecyclerView(orderByFoldersFirst(files))
                }
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

    //______________________________________________________________________________________________





    // adapter interface implementation ////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    override fun onDownload(fileId: String, fileName : String) {
        lifecycleCoroutineScope.launch {
            googleDriveApi.downloadFileFromDrive(context, fileId, fileName)
        }
    }
    override fun onShare(webViewLink: String) {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Share File Link")
        sharingIntent.putExtra(Intent.EXTRA_TEXT, webViewLink)
        context.startActivity(Intent.createChooser(sharingIntent, "Share File Link"))
    }
    override fun onDelete(fileId: String) {
        lifecycleCoroutineScope.launch(Dispatchers.IO) {
            val isDeleted = googleDriveApi.deleteFolder(fileId)
            if (isDeleted){
                withContext(Dispatchers.Main){
                    getFiles(currentNamesPath.last(), currentIdsPath.last())
                }
            }
        }
    }
    override fun onOpenFile(webContentLink: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(webContentLink)
        intent.setPackage("com.google.android.apps.docs") // Specify the package name of Google Drive app
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }
    override fun onOpenFolder(folderId: String, folderName : String) {
        navigateForward(folderId, folderName)
    }
    override fun copyFilePath(fileName: String) {
        var formattedPath = ""
        val pathList = currentNamesPath
        pathList.add(fileName)
        for (name in pathList){
            formattedPath += buildString {
                append("/")
                append(name)
            }
        }
        val clip = ClipData.newPlainText("", formattedPath)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(context, "Text copied!", Toast.LENGTH_SHORT).show()
    }

    //______________________________________________________________________________________________






    // navigation functions ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    fun navigateBack(){
        currentIdsPath.remove(currentIdsPath.last())
        currentNamesPath.remove(currentNamesPath.last())
        getFiles(currentNamesPath.last(), currentIdsPath.last())
    }
    private fun navigateForward(folderId: String, folderName : String){
        currentIdsPath.add(folderId)
        currentNamesPath.add(folderName)
        getFiles(folderName,  folderId)
    }

    //______________________________________________________________________________________________






    // dialogs /////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private fun showFileCreateDialog() {
        createFolderDialog.showCreateFolderDialog { folderName ->
            createFolder(folderName, currentIdsPath.last())
        }
    }

    //______________________________________________________________________________________________






    // UI updates //////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private fun updateRecyclerView(files: List<FileDriveItem>?) {
        if (::swipeRefreshLayout.isInitialized && swipeRefreshLayout.isRefreshing){
            this@GoogleDriveFileManager.swipeRefreshLayout.isRefreshing = false
        }
        if (files!=null){
            adapter.updateData(files)
            adapter.hideLoading()
        }
    }
    private fun updateToolbar(folderName: String, folderId:String) {
        if (::toolbar.isInitialized){
            toolbar.navigationIcon =  if (folderId == this.rootFolderId) null else
                ContextCompat.getDrawable(context, R.drawable.icon_arrow_left)
            toolbar.title = folderName
        }else{
            throw DriveUiElementsMissing("toolbar property is not initialized")
        }
    }
    private fun updateTextPathView() {
        if (::toolbar.isInitialized){
            var formattedPath = ""
            for (name in currentNamesPath){
                formattedPath += buildString {
                    append("/")
                    append(name)
                }
            }
            if(useNavigationPath) this.toolbar.subtitle = formattedPath
        }else{
            throw DriveUiElementsMissing("toolbar property is not initialized")
        }
    }

    //______________________________________________________________________________________________






    // public user input functions ////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    fun setRootFileName(rootFileName: String): GoogleDriveFileManager {
        if (::toolbar.isInitialized){
            this.currentNamesPath[0] = rootFileName
            if(useNavigationPath) this.toolbar.subtitle = buildString {
                append("$rootFileName/")
            }
            this.toolbar.title = rootFileName
        }else{
            throw DriveUiElementsMissing("toolbar property is not initialized")
        }
        return this@GoogleDriveFileManager
    }
    fun setRecyclerView(recyclerView: RecyclerView): GoogleDriveFileManager {
        adapter = GdFilesAdapter(context, emptyList(), listOf(permissions), filePathCopyable)
        adapter.setFileOptionsInterface(this@GoogleDriveFileManager)
        adapter.setAccessFileListener(this@GoogleDriveFileManager)
        adapter.setAccessFolderListener(this@GoogleDriveFileManager)
        recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter
        this.recyclerView = recyclerView
        return this@GoogleDriveFileManager
    }
    fun setRefreshableRecyclerView(swipeRefreshLayout: SwipeRefreshLayout, recyclerView: RecyclerView): GoogleDriveFileManager {
        adapter = GdFilesAdapter(context, emptyList(), listOf(permissions), filePathCopyable)
        adapter.setFileOptionsInterface(this@GoogleDriveFileManager)
        adapter.setAccessFileListener(this@GoogleDriveFileManager)
        adapter.setAccessFolderListener(this@GoogleDriveFileManager)
        recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter
        this.recyclerView = recyclerView
        this.swipeRefreshLayout=swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = true
            Log.e("check", "$currentNamesPath, $currentIdsPath")
            getFiles(currentNamesPath.last(), currentIdsPath.last())
        }
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
    fun initialize() {
        if (::rootFolderId.isInitialized){
            getFiles(currentNamesPath[0], rootFolderId)
        }else{
            throw DriveRootException("Root folder id not provided")
        }
    }
    fun setRootFileId(rootFileId: String): GoogleDriveFileManager {
        this.rootFolderId = rootFileId
        currentIdsPath.add(0, rootFileId)
        return this@GoogleDriveFileManager
    }
    fun setFilePathCopyable(filePathCopyable : Boolean): GoogleDriveFileManager {
        this.filePathCopyable = filePathCopyable
        adapter.updateFilePathCopyable(filePathCopyable)
        return this@GoogleDriveFileManager
    }
    fun activateNavigationPath(useNavigationPath : Boolean): GoogleDriveFileManager {
        if (::toolbar.isInitialized){
            this.useNavigationPath = useNavigationPath
            return this@GoogleDriveFileManager
        }else{
            throw DriveUiElementsMissing("toolbar property is not initialized")
        }
    }

    //______________________________________________________________________________________________






    // utility functions ///////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private fun orderByFoldersFirst(files: List<FileDriveItem>): List<FileDriveItem> {
        return files.sortedWith(compareBy<FileDriveItem> { it.mimeType != "application/vnd.google-apps.folder" }
            .thenBy { it.fileName })
    }
    private fun setSearchViewFunctionality(menu: Menu?) {
        val searchItem = menu?.findItem(R.id.btn_search)
        val searchView = searchItem?.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()){
                    queryFiles(query)
                }
                return true
            }
            override fun onQueryTextChange(query: String?): Boolean {
                return false
            }
        })
    }
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

    //______________________________________________________________________________________________

}
package com.az.drivepreviewlibrary.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.view.Menu
import android.webkit.MimeTypeMap
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.az.drivepreviewlibrary.R
import com.az.drivepreviewlibrary.presentation.adapters.GdFilesAdapter
import com.az.drivepreviewlibrary.presentation.adapters.GdFilesAdapter.AccessFileListener
import com.az.drivepreviewlibrary.presentation.adapters.GdFilesAdapter.AccessFolderListener
import com.az.drivepreviewlibrary.presentation.adapters.GdFilesAdapter.FileOptions
import com.az.drivepreviewlibrary.api.GoogleDriveApi
import com.az.drivepreviewlibrary.data.exceptions.DriveManagerException
import com.az.drivepreviewlibrary.data.exceptions.DriveRootException
import com.az.drivepreviewlibrary.data.exceptions.DriveUiElementsMissing
import com.az.drivepreviewlibrary.data.exceptions.MimeTypeException
import com.az.drivepreviewlibrary.data.managers.FilePickerListener
import com.az.drivepreviewlibrary.data.managers.GdCredentialsProvider
import com.az.drivepreviewlibrary.data.models.FileDriveItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


/**
 * Google drive file manager
 *
 * @property context
 * @property lifecycleCoroutineScope
 * @property permissions
 * @constructor
 *
 * @param gdCredentialsProvider
 * @param applicationName
 */
class GoogleDriveFileManager(
    private val context: Context,
    private val lifecycleCoroutineScope: LifecycleCoroutineScope,
    private val permissions : Permissions,
    gdCredentialsProvider: GdCredentialsProvider,
    applicationName: String,
) : FileOptions, AccessFileListener, AccessFolderListener {

    private lateinit var adapter: GdFilesAdapter
    private var googleDriveApi: com.az.drivepreviewlibrary.api.GoogleDriveApi =
        com.az.drivepreviewlibrary.api.GoogleDriveApi(
            gdCredentialsProvider = gdCredentialsProvider,
            NotificationLauncher(context), appName = applicationName
        )
    private var clipboardManager: ClipboardManager
    private val currentIdsPath = mutableListOf<String>()
    private var currentNamesPath = mutableListOf("Drive Folder")
    private lateinit var toolbar: Toolbar
    private val createFolderDialog =  CreateFileDialog(context)
    private val loadingDialog = LoadingDialog(context)
    private lateinit var swipeRefreshLayout :   SwipeRefreshLayout
    private lateinit var recyclerView : RecyclerView
    private lateinit var rootFolderId : String
    private var filePathCopyable = false
    private var useNavigationPath = false
    private lateinit var filePickerListener: FilePickerListener


    init {
        val clipboardServiceClass = Class.forName("android.content.ClipboardManager")
        clipboardManager = getSystemService(context, clipboardServiceClass) as ClipboardManager
    }


    // API calling functions ///////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Retrieves files from Google Drive for the specified folder and updates the RecyclerView with the obtained files.
     *
     * Displays a loading indicator through the adapter before fetching the files asynchronously using a coroutine.
     * Upon successful retrieval of files, updates the RecyclerView with the files ordered by folders first. Also updates
     * the toolbar with the specified folder's name and ID and updates the text path view accordingly.
     *
     * @param folderName The name of the folder.
     * @param folderId The ID of the folder from which files are to be retrieved.
     */

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

    /**
     * Queries files in Google Drive based on the provided query string and updates the RecyclerView with the obtained files.
     *
     * Displays a loading indicator through the adapter before executing the query asynchronously using a coroutine.
     * Upon successful retrieval of files, updates the RecyclerView with the files ordered by folders first.
     *
     * @param query The query string used to search for files in Google Drive.
     */

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

    /**
     * Creates a new folder with the specified name within the given parent folder in Google Drive.
     *
     * Executes the folder creation asynchronously using a coroutine.
     * Upon successful creation of the folder, hides the create folder dialog.
     *
     * @param folderName The name of the folder to be created.
     * @param parentFolderId The ID of the parent folder in which the new folder will be created.
     */
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
    /**
     * Initiates the download of a file from Google Drive with the specified file ID and file name.
     *
     * Initiates the file download asynchronously using a coroutine.
     *
     * @param fileId The ID of the file to be downloaded from Google Drive.
     * @param fileName The name to be used for the downloaded file.
     */
    override fun onDownload(fileId: String, fileName : String) {
        lifecycleCoroutineScope.launch {
            googleDriveApi.downloadFileFromDrive(fileId, fileName)
        }
    }

    /**
     * Initiates the sharing of a file link using an Intent.
     *
     * Creates an Intent to share a file link with the provided WebView link.
     * Starts an activity to allow the user to choose from available sharing options.
     *
     * @param webViewLink The WebView link of the file to be shared.
     */
    override fun onShare(webViewLink: String) {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Share File Link")
        sharingIntent.putExtra(Intent.EXTRA_TEXT, webViewLink)
        context.startActivity(Intent.createChooser(sharingIntent, "Share File Link"))
    }

    /**
     * Initiates the deletion of a file or folder from Google Drive with the specified file/folder ID.
     *
     * Initiates the deletion process asynchronously using a coroutine with IO dispatcher.
     * Upon successful deletion, retrieves and updates the files in the current folder.
     *
     * @param fileId The ID of the file or folder to be deleted from Google Drive.
     */
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

    /**
     * Initiates the opening of a file using the Google Drive app with the provided web content link.
     *
     * Creates an Intent to open the file using the Google Drive app.
     * Specifies the package name of the Google Drive app for handling the file.
     * Starts the activity to view the file in the Google Drive app.
     *
     * @param webContentLink The web content link of the file to be opened.
     */
    override fun onOpenFile(webContentLink: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(webContentLink)
        intent.setPackage("com.google.android.apps.docs") // Specify the package name of Google Drive app
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    /**
     * Initiates the navigation to open a folder within the application.
     *
     * Navigates forward to the specified folder ID and folder name within the application.
     *
     * @param folderId The ID of the folder to be opened.
     * @param folderName The name of the folder to be opened.
     */
    override fun onOpenFolder(folderId: String, folderName : String) {
        navigateForward(folderId, folderName)
    }

    /**
     * Copies the file path to the clipboard.
     *
     * Formats the file path by appending each folder and the file name with a "/" separator.
     * Copies the formatted file path to the clipboard.
     * Displays a toast message indicating that the text has been copied.
     *
     * @param fileName The name of the file to be included in the file path.
     */
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
    /**
     * Navigate back
     *
     * @param onRootReached
     */
    fun navigateBack(onRootReached : (() -> Unit)? = null){
        if (currentIdsPath.size != 1){
            currentIdsPath.remove(currentIdsPath.last())
            currentNamesPath.remove(currentNamesPath.last())
            getFiles(currentNamesPath.last(), currentIdsPath.last())
        }else if(onRootReached != null) {
            onRootReached.invoke()
        }else{
            Toast.makeText(context, "Root reached!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Navigates forward within the application to the specified folder.
     *
     * Adds the provided folder ID and name to the current path.
     * Retrieves files from the specified folder and updates the UI accordingly.
     *
     * @param folderId The ID of the folder to navigate to.
     * @param folderName The name of the folder to navigate to.
     */
    private fun navigateForward(folderId: String, folderName : String){
        currentIdsPath.add(folderId)
        currentNamesPath.add(folderName)
        getFiles(folderName,  folderId)
    }

    //______________________________________________________________________________________________






    // dialogs /////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Displays the file creation dialog.
     *
     * Shows the dialog for creating a new folder. When a folder name is provided through the callback,
     * initiates the creation of a new folder within the current folder.
     */
    private fun showFileCreateDialog() {
        createFolderDialog.showCreateFolderDialog { folderName ->
            createFolder(folderName, currentIdsPath.last())
        }
    }

    //______________________________________________________________________________________________






    // UI updates //////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Updates the RecyclerView with the provided list of files.
     *
     * Checks if the SwipeRefreshLayout is initialized and refreshing, and stops the refresh animation if so.
     * Updates the RecyclerView adapter with the new list of files and hides the loading indicator.
     *
     * @param files The list of files to be displayed in the RecyclerView.
     */
    private fun updateRecyclerView(files: List<FileDriveItem>?) {
        if (::swipeRefreshLayout.isInitialized && swipeRefreshLayout.isRefreshing){
            this@GoogleDriveFileManager.swipeRefreshLayout.isRefreshing = false
        }
        if (files!=null){
            adapter.updateData(files)
            adapter.hideLoading()
        }
    }
    /**
     * Updates the toolbar with the specified folder name and ID.
     *
     * Checks if the toolbar is initialized. If so, sets the navigation icon based on whether the folder is the root folder.
     * Sets the title of the toolbar to the provided folder name.
     *
     * @param folderName The name of the folder to be displayed in the toolbar.
     * @param folderId The ID of the folder.
     *
     * @throws DriveUiElementsMissing if the toolbar property is not initialized.
     */
    private fun updateToolbar(folderName: String, folderId:String) {
        if (::toolbar.isInitialized){
            toolbar.navigationIcon =  if (folderId == this.rootFolderId) null else
                ContextCompat.getDrawable(context, R.drawable.icon_arrow_left)
            toolbar.title = folderName
        }else{
            throw DriveUiElementsMissing("toolbar property is not initialized")
        }
    }

    /**
     * Updates the text path view with the current navigation path.
     *
     * Checks if the toolbar is initialized. If so, formats the current navigation path and sets it as the toolbar subtitle,
     * if enabled. The navigation path is constructed by concatenating folder names with a "/" separator.
     *
     * @throws DriveUiElementsMissing if the toolbar property is not initialized.
     */
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
    /**
     * Set root file name
     *
     * @param rootFileName
     * @return
     */
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

    /**
     * Set recycler view
     *
     * @param recyclerView
     * @return
     */
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

    /**
     * Set refreshable recycler view
     *
     * @param swipeRefreshLayout
     * @param recyclerView
     * @return
     */
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
            getFiles(currentNamesPath.last(), currentIdsPath.last())
        }
        return this@GoogleDriveFileManager
    }

    /**
     * Set access file listener
     *
     * @param accessFileListener
     * @return
     */
    fun setAccessFileListener(accessFileListener: AccessFileListener): GoogleDriveFileManager {
        adapter.setAccessFileListener(accessFileListener)
        return this@GoogleDriveFileManager
    }


    /**
     * Set action bar
     *
     * @param toolbar
     * @return
     */
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
            navigateBack(null)
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
                R.id.btn_create_file ->{
                    launchFilePicker()
                    true
                }
                else -> false
            }
        }
        return this@GoogleDriveFileManager
    }


    /**
     * Initialize
     *
     */
    fun initialize() {
        if (::rootFolderId.isInitialized){
            getFiles(currentNamesPath[0], rootFolderId)
        }else{
            throw DriveRootException("Root folder id not provided")
        }
    }

    /**
     * Set root file id
     *
     * @param rootFileId
     * @return
     */
    fun setRootFileId(rootFileId: String): GoogleDriveFileManager {
        this.rootFolderId = rootFileId
        currentIdsPath.add(0, rootFileId)
        return this@GoogleDriveFileManager
    }

    /**
     * Set file path copyable
     *
     * @param filePathCopyable
     * @return
     */
    fun setFilePathCopyable(filePathCopyable : Boolean): GoogleDriveFileManager {
        this.filePathCopyable = filePathCopyable
        adapter.updateFilePathCopyable(filePathCopyable)
        return this@GoogleDriveFileManager
    }

    /**
     * Activate navigation path
     *
     * @param useNavigationPath
     * @return
     */
    fun activateNavigationPath(useNavigationPath : Boolean): GoogleDriveFileManager {
        if (::toolbar.isInitialized){
            this.useNavigationPath = useNavigationPath
            return this@GoogleDriveFileManager
        }else{
            throw DriveUiElementsMissing("toolbar property is not initialized")
        }
    }

    /**
     * Set file picker listener
     *
     * @param filePickerListener
     * @return
     */
    fun setFilePickerListener(filePickerListener: FilePickerListener): GoogleDriveFileManager {
        this.filePickerListener = filePickerListener
        return this@GoogleDriveFileManager
    }


    /**
     * Upload file to drive
     *
     * @param file
     */
    fun uploadFileToDrive(file:File) {
        lifecycleCoroutineScope.launch {
            this@GoogleDriveFileManager.loadingDialog.showLoadingDialog()
            val mimeType = getMimeTypeFromExtension(file) ?: throw MimeTypeException("File With An Unsupported MimeType")
            googleDriveApi.createFileOnDrive(
                file,
                currentIdsPath.last(),
                mimeType
            )
            file.delete()
            this@GoogleDriveFileManager.loadingDialog.hideLoadingDialog()
        }
    }

    //______________________________________________________________________________________________





    // utility functions ///////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Orders files with folders first in the list.
     *
     * Sorts the list of files with folders first followed by other file types,
     * and then alphabetically by file name.
     *
     * @param files The list of files to be ordered.
     * @return The ordered list of files with folders first.
     */
    private fun orderByFoldersFirst(files: List<FileDriveItem>): List<FileDriveItem> {
        return files.sortedWith(compareBy<FileDriveItem> { it.mimeType != "application/vnd.google-apps.folder" }
            .thenBy { it.fileName })
    }

    /**
     * Sets up search functionality for the SearchView in the toolbar menu.
     *
     * Retrieves the SearchView from the menu and sets query text listener to handle search queries.
     * Initiates file search when a query is submitted.
     * Refreshes the file list when the search view is closed.
     *
     * @param menu The menu containing the SearchView item.
     */
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
        searchView.setOnCloseListener {
            getFiles(currentNamesPath.last(), currentIdsPath.last())
            false
        }
    }

    /**
     * Sets custom styles for the SearchView widget in the menu.
     *
     * @param menu The menu containing the SearchView widget.
     */
    private fun setSearchViewStyle(menu: Menu) {
        val searchItem = menu.findItem(R.id.btn_search)
        val searchView = searchItem.actionView as SearchView

        searchView.queryHint = "search"

        val editText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        editText.setTextColor(ContextCompat.getColor(context, R.color.black))
        editText.setHintTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))

        val closeButton = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        val searchButton = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_button)
        closeButton.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.black))
        searchButton.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.black))
    }


    /**
     * Launches the file picker.
     *
     * Checks if the file picker listener is initialized.
     * If initialized, launches the file picker.
     * Otherwise, throws an exception indicating that the FilePickerListener interface is not implemented.
     */
    private fun launchFilePicker() {
        if (::filePickerListener.isInitialized){
            filePickerListener.launchFilePicker()
        }else{
            throw DriveManagerException("FilePickerListener interface is not implemented")
        }
    }


    /**
     * Retrieves the MIME type of a file based on its extension.
     *
     * Extracts the extension from the file name and retrieves the corresponding MIME type.
     *
     * @param file The file for which the MIME type is to be retrieved.
     * @return The MIME type of the file, or null if no extension is found.
     */
    private fun getMimeTypeFromExtension(file: File): String? {
        val extension = file.extension
        if (extension.isEmpty()) {
            return null // No extension found
        }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }



    //______________________________________________________________________________________________

}
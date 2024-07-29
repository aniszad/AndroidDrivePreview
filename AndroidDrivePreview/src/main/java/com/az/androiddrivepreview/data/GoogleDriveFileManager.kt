package com.az.androiddrivepreview.data

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.view.Menu
import android.webkit.MimeTypeMap
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.az.androiddrivepreview.R
import com.az.androiddrivepreview.adapters.GdFilesAdapter
import com.az.androiddrivepreview.adapters.GdFilesAdapter.AccessFileListener
import com.az.androiddrivepreview.adapters.GdFilesAdapter.AccessFolderListener
import com.az.androiddrivepreview.adapters.GdFilesAdapter.FileOptions
import com.az.androiddrivepreview.api.GoogleDriveApi
import com.az.androiddrivepreview.data.exceptions.DriveRootException
import com.az.androiddrivepreview.data.exceptions.DriveUiElementsMissing
import com.az.androiddrivepreview.data.exceptions.MimeTypeException
import com.az.androiddrivepreview.data.managers.FilePickerListener
import com.az.androiddrivepreview.data.managers.GdCredentialsProvider
import com.az.androiddrivepreview.data.models.FileDriveItem
import com.az.androiddrivepreview.utils.Dialogs
import com.az.androiddrivepreview.utils.NotificationLauncher
import com.az.androiddrivepreview.utils.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream


/**
 * Google drive file manager
 *
 * @property context
 * @property lifecycleCoroutineScope
 * @property permissions
 * @constructor
 *
 * @param gdCredentialsProvider
 */
class GoogleDriveFileManager(
    private val context: Context,
    private val lifecycleCoroutineScope: LifecycleCoroutineScope,
    private val permissions : Permissions,
    gdCredentialsProvider: GdCredentialsProvider,
) : FileOptions, AccessFileListener, AccessFolderListener {
    private var darkMode: Boolean = false
    private val filePickerLauncher = (context as AppCompatActivity).registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val uri: Uri? = data?.data
            if (uri != null) {
                try {
                    initiateUpload(uri)
                } catch (e: Exception) {
                    Log.e("FilePath", "Error copying file", e)
                }
            }
        }

    }

    private lateinit var adapter: GdFilesAdapter
    private var googleDriveApi: GoogleDriveApi =
        GoogleDriveApi(
            context,
            gdCredentialsProvider = gdCredentialsProvider,
            NotificationLauncher(context), appName = getString(context, R.string.app_name)
        )
    private var clipboardManager: ClipboardManager
    private val currentIdsPath = mutableListOf<String>()
    private var currentNamesPath = mutableListOf("Drive Folder")
    private lateinit var toolbar: Toolbar
    private val mCustomDialogs =  Dialogs(context)
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
                    mCustomDialogs.hideCreateFolderDialog()
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
            googleDriveApi.downloadFileFromDrive(fileId, fileName, currentNamesPath)
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
        mCustomDialogs.showCreateFolderDialog { folderName ->
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

            val iconTintColor = if (darkMode) Color.WHITE else Color.BLACK
            toolbar.navigationIcon?.setTintList(ColorStateList.valueOf(iconTintColor))
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
     * Set root folder name
     *
     * @param rootFileName
     * @return
     */
    fun setRootFolderName(rootFileName: String): GoogleDriveFileManager {
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
            Permissions.USER ->{
                toolbar.inflateMenu(R.menu.strict_toolbar_menu)
            }
            Permissions.STRICT ->{
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
     * Set download path
     *
     * @param downloadPath
     */
    fun setDownloadPath(downloadPath : String){
        googleDriveApi.setDownloadPath(downloadPath)
    }


    // public function so the user can initiate upload using a custom file picker
    fun initiateUpload(fileUri : Uri){
        val inputStream = context.contentResolver.openInputStream(fileUri)!!

        // Determine a destination path within your app's storage
        val fileName = getFileName(fileUri) // Get the file name from the URI (optional)
        val destinationPath = getInternalStoragePath(fileName) // Or getExternalStoragePath(fileName)


        val outputStream = FileOutputStream(destinationPath)
        val buffer = ByteArray(1024)
        var readBytes: Int

        while (inputStream.read(buffer).also { readBytes = it } != -1) {
            outputStream.write(buffer, 0, readBytes)
        }

        inputStream.close()
        outputStream.close()

        // Use the destinationPath for upload operations
        Log.d("FilePath", "File copied to: $destinationPath")
        showFileUploadDialog(File(destinationPath))
    }


    fun setThemeMode(darkMode: Boolean): GoogleDriveFileManager {
        this.darkMode = darkMode
        adapter.setDarkMode(darkMode)

        val textColor = if (darkMode) Color.WHITE else Color.BLACK
        val iconTintColor = if (darkMode) Color.WHITE else Color.BLACK
        val hintColor = if (darkMode) Color.WHITE else Color.DKGRAY

        toolbar.setTitleTextColor(textColor)
        toolbar.navigationIcon?.setTintList(ColorStateList.valueOf(iconTintColor))

        val searchItem = toolbar.menu.findItem(R.id.btn_search)
        val btnCreateFolder = toolbar.menu.findItem(R.id.btn_create_folder)
        val btnContribute = toolbar.menu.findItem(R.id.btn_create_file)
        val searchView = searchItem.actionView as SearchView

        searchView.queryHint = "search"

        val editText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        editText.setTextColor(textColor)
        editText.setHintTextColor(hintColor)

        val closeButton = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        val searchButton = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_button)
        closeButton.imageTintList = ColorStateList.valueOf(iconTintColor)
        searchButton.imageTintList = ColorStateList.valueOf(iconTintColor)
        btnContribute.iconTintList = ColorStateList.valueOf(iconTintColor)
        btnCreateFolder.iconTintList = ColorStateList.valueOf(iconTintColor)

        return this@GoogleDriveFileManager
    }
    /**
     * Upload file to drive
     *
     * @param file
     */
    private fun uploadFileToDrive(file:File) {
        try {
            lifecycleCoroutineScope.launch {
                val mimeType = getMimeTypeFromExtension(file)
                    ?: throw MimeTypeException("File With An Unsupported MimeType")
                googleDriveApi.createFileOnDrive(
                    file,
                    currentIdsPath.last(),
                    mimeType
                )
                file.delete()
                withContext(Dispatchers.Main){
                    mCustomDialogs.hideUploadFileDialog("${file.name} uploaded successfully")
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to upload file", Toast.LENGTH_SHORT).show()
            Log.e("FileUpload", e.stackTraceToString())
        }
    }


    /**
     * Show file upload dialog
     *
     * @param file
     */
    private fun showFileUploadDialog(file:File){
        mCustomDialogs.showUploadFileDialog(file.name) { uploadFileToDrive(file) }
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
     * Otherwise, launches the default file picker
     */
    private fun launchFilePicker() {
        if (::filePickerListener.isInitialized){
            filePickerListener.launchFilePicker()
        }else{
            openFilePicker()
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
    private fun getInternalStoragePath(fileName: String): String {
        val filesDir = context.filesDir
        return File(filesDir, fileName).absolutePath
    }
    private fun getFileName(uri: Uri): String {
        val fileName=""
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            return if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (columnIndex != -1) {
                    Log.e("filename", fileName)
                    cursor.getString(columnIndex)
                } else {
                    ""
                }
            }else{
                ""
            }
        }
        Log.e("filename", fileName)
        return fileName
    }
    private fun openFilePicker() {
        val intent = getFileChooserIntent()
        filePickerLauncher.launch(intent)
    }
    private fun getFileChooserIntent(): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        return intent
    }






    //______________________________________________________________________________________________

}
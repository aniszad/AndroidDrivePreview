package com.az.androiddrivepreview

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.az.androiddrivepreview.data.GoogleDriveFileManager
import com.az.androiddrivepreview.databinding.ActivityMainBinding
import com.az.androiddrivepreview.utils.Permissions
import java.io.File


class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private lateinit var gdm : GoogleDriveFileManager
    private val sharedPref = SharedPref(this@MainActivity)

    private val activityDownloadFolderAccessResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            if (isFolderNameCorrect(uri)){
                sharedPref.isDownloadFolderAccessGranted = true
                gdm.setDownloadPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path+ "/${
                    getString(
                        R.string.app_name
                    )
                }")
            }else{
                Toast.makeText(this@MainActivity, "Please select the correct folder $uri", Toast.LENGTH_SHORT).show()
            }

        } else {
            Log.e("FolderAccess", "Permission denied, cannot create folder. Downloads are not possible.")
            sharedPref.isDownloadFolderAccessGranted = false
        }
    }



    private val requestWritePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                createDownloadFolder()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Permission denied, cannot create folder",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(binding.main.id)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Initialize GoogleDriveFileManager
        gdm = GoogleDriveFileManager(
            this@MainActivity,
            lifecycleScope, // lifecycle scope for launching coroutines
            Permissions.ADMIN,
            CredentialsProvider(),
        )

        // Set the recycler view, toolbar, root file id, root folder name, and the file picker listener
        gdm.setRecyclerView(binding.recyclerView) // set recycler view to display files
            .setActionBar(binding.toolbar) // set toolbar to display file name, actions, path
            .setRootFileId("1ZEmBUIPWUXr_nae82N7qQHudIFwaxRe5") // the id of the drive file to be displayed
            .setRootFolderName("Files Bank") // the root file name
            .activateNavigationPath(false) // set to true to display the path of the current directory
            .setFilePathCopyable(true) // set to true to allow the user to copy the path of the current directory
            .setThemeMode(true)
            .initialize() // initialize the GoogleDriveFileManager

        // Handle back press
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                gdm.navigateBack { this@MainActivity.finish() }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)


        /**
         * setupDownloadFolder
         *
         * This function is used to setup the download folder for the app.
         * including creating the folder and granting permission to it, and then
         * calling gdm.setDownloadPath() to set the download path.
         *
         * not implementing means using the default behavior which is downloading into :
         * "context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.path"
         *
         */
        //setupDownloadFolder()

    }

    private fun setupDownloadFolder() {
        val appFolderName = getString(R.string.app_name)
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val folderPath = File(downloadsDir, appFolderName)

        if (!folderPath.exists()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                requestFolderCreationPermission()
            }else{
                createDownloadFolder()
            }
        }else{
            if(!sharedPref.isDownloadFolderAccessGranted){
                openFolderForAccess(folderPath.toUri())
            }
        }
    }
    private fun createDownloadFolder() {
        val appFolderName = getString(R.string.app_name)
        // Folder doesn't exist, create it
        val relativePath = Environment.DIRECTORY_DOWNLOADS + "/" + appFolderName
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, appFolderName)
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        try {
            val folderUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            Log.e("FolderCreation", "$folderUri")
            if (folderUri!=null){
                openFolderForAccess(folderUri)
            }
        } catch (e: Exception) {
            Log.e("FolderCreation", "Failed to create folder: ${e.message}")
        }
    }
    private fun openFolderForAccess(folderUri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                .putExtra(DocumentsContract.EXTRA_INITIAL_URI, folderUri)
            activityDownloadFolderAccessResultLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e("FolderAccess", "No activity found to handle the intent")
        }
    }
    private fun requestFolderCreationPermission() {
        // Check if permissions have been granted
        if (!hasPermissions(this@MainActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
            // Request permissions
            requestWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            createDownloadFolder()
        }
    }
    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
    private fun isFolderNameCorrect(uri: Uri): Boolean {
        val documentFile = DocumentFile.fromTreeUri(this@MainActivity, uri)
        val folderName = documentFile?.name
        return folderName == getString(R.string.app_name)
    }
}
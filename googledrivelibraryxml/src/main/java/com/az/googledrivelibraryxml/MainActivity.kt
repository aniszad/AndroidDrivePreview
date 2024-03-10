package com.az.googledrivelibraryxml

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.az.googledrivelibraryxml.databinding.ActivityMainBinding
import com.az.googledrivelibraryxml.managers.FilePickerListener
import com.az.googledrivelibraryxml.utils.GoogleDriveFileManager
import com.az.googledrivelibraryxml.utils.Permissions
import com.google.api.client.http.FileContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.security.Permission

class MainActivity : AppCompatActivity(), FilePickerListener {

    private lateinit var binding : ActivityMainBinding

    private lateinit var gdm : GoogleDriveFileManager


    private val REQUEST_CODE_PICK_FILE = 123
    private val FILE_PICKER_REQUEST_CODE = 124
    // Function to extract MIME type from a file URI
    private fun getMimeType(contentResolver: ContentResolver, fileUri: Uri): String? {
        return contentResolver.getType(fileUri)
    }

    // Function to handle the result of the file picker
    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val uri: Uri? = data?.data
                if (uri != null) {
                    // Handle the selected file URI here
                    // Process the selected file URI as needed
                    val contentResolver: ContentResolver = contentResolver
                    val fileUri: Uri = uri

                    // Extract MIME type
                    val mimeType = getMimeType(contentResolver, fileUri)

                    // Publish the file to Google Drive
                    if (mimeType != null) {
                        val file = File(uri.path) // Assuming the file URI represents a local file path
                        GlobalScope.launch(Dispatchers.Main) {
                            val fileId = gdm.uploadFileToDrive(file)
                            if (fileId != null) {
                                // File published successfully
                                Toast.makeText(this@MainActivity, "File published successfully.", Toast.LENGTH_SHORT).show()
                            } else {
                                // Failed to publish file
                                Toast.makeText(this@MainActivity, "Failed to publish file.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        // Unable to determine MIME type
                        Toast.makeText(this@MainActivity, "Failed to determine MIME type.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // File picking was canceled or failed
                    Toast.makeText(this@MainActivity, "File picking canceled or failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun getFileExtension(contentResolver: ContentResolver, uri: Uri): String? {
        val mimeTypeMap = MimeTypeMap.getSingleton()
        val mimeType = contentResolver.getType(uri)
        return mimeTypeMap.getExtensionFromMimeType(mimeType)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        gdm = GoogleDriveFileManager(
            this@MainActivity,
            lifecycleScope, // lifecycle scope for launching coroutines
            Permissions.ADMIN,
            CredentialsProvider(),
            "test",
        )
        gdm.setRecyclerView(binding.recyclerView) // set recycler view to display files
            .setActionBar(binding.toolbar) // set toolbar to display file name, actions, path
            .setRootFileId("1ZEmBUIPWUXr_nae82N7qQHudIFwaxRe5") // the id of the drive file to be displayed
            .setRootFileName("Files Bank") // the root file name
            .activateNavigationPath(false) // set to true to display the path of the current directory
            .setFilePathCopyable(true) // set to true to allow the user to copy the path of the current directory
            .setFilePickerListener(this@MainActivity)
            .initialize() // initialize the GoogleDriveFileManager


        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Call the navigateBack() function of your GoogleDriveManager class
                gdm.navigateBack()
            }
        }

        // Add the onBackPressedCallback to the onBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, callback)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(intent)
        } else {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }






    }

    override fun launchFilePicker() {
        openFilePicker()
    }

    private fun openFilePicker() {
        val intent = getFileChooserIntent()
        startActivityForResult(intent, FILE_PICKER_REQUEST_CODE)
    }
    private fun getFileChooserIntent(): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        return intent
    }


    private fun getFileContent(uri: Uri): FileContent? {
        val contentResolver = applicationContext.contentResolver
        val mimeType = contentResolver.getType(uri) ?: return null
        val fileDescriptor = contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor ?: return null
        val file = File.createTempFile("temp", null).apply {
            deleteOnExit()
            //writeFromDescriptor(fileDescriptor)
        }
        return FileContent(mimeType, file)
    }




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            val uri = data?.data ?: return

            try {
                val resolver = contentResolver
                val inputStream = resolver.openInputStream(uri) ?: return

                // Determine a destination path within your app's storage
                val fileName = getFileName(uri) // Get the file name from the URI (optional)
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
                gdm.uploadFileToDrive(File(destinationPath))

            } catch (e: Exception) {
                Log.e("FilePath", "Error copying file", e)
            }
        }
    }

    // Helper functions to get file name and storage paths (implement these functions)
    private fun getFileName(uri: Uri): String {
        val fileName=""
        val cursor = contentResolver.query(uri, null, null, null, null)
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

    private fun getInternalStoragePath(fileName: String): String {
        val context = applicationContext
        val filesDir = context.filesDir
        return File(filesDir, fileName).absolutePath
    }


    // Consider using SAF (Storage Access Framework) for external storage on Android 10+
    /*private fun getExternalStoragePath(fileName: String): String {
        // Implement logic to get a path within your app's designated directory on external storage
        // This might require requesting storage permissions
    }

     */






}
package com.az.googledrivelibraryxml

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.os.BuildCompat
import androidx.lifecycle.lifecycleScope
import com.az.googledrivelibraryxml.adapters.GdFilesAdapter
import com.az.googledrivelibraryxml.databinding.ActivityMainBinding
import com.az.googledrivelibraryxml.utils.GoogleDriveFileManager
import com.az.googledrivelibraryxml.utils.Permissions

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private val REQUEST_CODE_SAF = 101

    private lateinit var gdm : GoogleDriveFileManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gdm = GoogleDriveFileManager(
            this@MainActivity,
            "1ZEmBUIPWUXr_nae82N7qQHudIFwaxRe5",
            lifecycleScope,
            Permissions.ADMIN,
            "res/raw/credentials.json",
            "test",

        )
        gdm.setRecyclerView(binding.recyclerView)
            .setActionBar(binding.toolbar)
            .setPathTextView(binding.tvPath)
            .setRootFileName("Files Bank")


        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Call the navigateBack() function of your GoogleDriveManager class
                gdm.navigateBack()
            }
        }

        // Add the onBackPressedCallback to the onBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, callback)

        checkStoragePermission()

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

    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermission()
        } else {
            // Permission is already granted, proceed with storage access
            // Your code to access storage goes here
        }
    }

    private fun requestStoragePermission() {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE
        )
        requestPermissions(permissions, STORAGE_PERMISSION_CODE)
    }

    private val STORAGE_PERMISSION_CODE = 101 // Choose a unique code

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with storage access
                // Your code to access storage goes here
            } else {
                // Permission denied, inform the user
                Toast.makeText(this, "Storage permission is required for this feature", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SAF && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            // Use the granted URI to access the selected file
        }
    }

}
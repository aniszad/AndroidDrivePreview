package com.az.googledrivelibraryxml

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.az.googledrivelibraryxml.databinding.ActivityMainBinding
import com.az.googledrivelibraryxml.utils.GoogleDriveFileManager
import com.az.googledrivelibraryxml.utils.Permissions

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    private lateinit var gdm : GoogleDriveFileManager

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
            .initialize() // initialize the GoogleDriveFileManager


        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Call the navigateBack() function of your GoogleDriveManager class
                gdm.navigateBack()
            }
        }

        // Add the onBackPressedCallback to the onBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, callback)


        /*  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(intent)
        } else {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }

         */






    }


}
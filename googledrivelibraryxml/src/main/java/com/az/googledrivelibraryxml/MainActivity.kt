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
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        gdm = GoogleDriveFileManager(
            this@MainActivity,
            "1ZEmBUIPWUXr_nae82N7qQHudIFwaxRe5",
            lifecycleScope,
            Permissions.USER,
            CredentialsProvider(),
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


        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
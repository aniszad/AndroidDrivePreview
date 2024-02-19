package com.az.googledrivelibraryxml

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.az.googledrivelibraryxml.adapters.GdFilesAdapter
import com.az.googledrivelibraryxml.databinding.ActivityMainBinding
import com.az.googledrivelibraryxml.utils.GoogleDriveFileManager
import com.az.googledrivelibraryxml.utils.Permissions

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    private lateinit var gdm : GoogleDriveFileManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gdm = GoogleDriveFileManager(
            this@MainActivity,
            "1ZEmBUIPWUXr_nae82N7qQHudIFwaxRe5",
            lifecycleScope,
            Permissions.USER,
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

    }

}
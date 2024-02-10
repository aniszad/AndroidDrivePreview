package com.az.googledrivelibraryxml

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.az.googledrivelibraryxml.databinding.ActivityMainBinding
import com.az.googledrivelibraryxml.utils.GoogleDriveFileManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gdm = GoogleDriveFileManager(this@MainActivity, "res/raw/credentials.json", "test", "1ZEmBUIPWUXr_nae82N7qQHudIFwaxRe5")
        gdm.setRecyclerView(binding.recyclerView)
        gdm.setActionBar(binding.toolbar)


    }
}
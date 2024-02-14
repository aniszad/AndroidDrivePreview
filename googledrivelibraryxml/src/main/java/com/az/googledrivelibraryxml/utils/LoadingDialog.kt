package com.az.googledrivelibraryxml.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import com.az.googledrivelibraryxml.databinding.CreateFolderDialogLayoutBinding

class LoadingDialog(private val context: Context) {
    private lateinit var loadingDialog : Dialog
    fun showCreateFolderDialog(){
        loadingDialog = Dialog(context)
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val bindingCfDialog = CreateFolderDialogLayoutBinding.inflate(LayoutInflater.from(context))
        loadingDialog.setContentView(bindingCfDialog.root)
        loadingDialog.show()
    }

    private fun hideCreateFolderDialog(){
        this.loadingDialog.hide()
    }
}
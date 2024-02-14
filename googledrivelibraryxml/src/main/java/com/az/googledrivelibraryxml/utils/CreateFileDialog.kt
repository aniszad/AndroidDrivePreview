package com.az.googledrivelibraryxml.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import com.az.googledrivelibraryxml.databinding.CreateFolderDialogLayoutBinding

class CreateFileDialog(private val context: Context) {
    private lateinit var mCreateFolderDialog : Dialog
    fun showCreateFolderDialog(callback : (folderName : String) -> Unit){
        mCreateFolderDialog = Dialog(context)
        mCreateFolderDialog.setCancelable(true)
        mCreateFolderDialog.setCanceledOnTouchOutside(true)
        mCreateFolderDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val bindingCfDialog = CreateFolderDialogLayoutBinding.inflate(LayoutInflater.from(context))
        mCreateFolderDialog.setContentView(bindingCfDialog.root)
        bindingCfDialog.apply {
            btnCreateFolder.setOnClickListener {
                val newFolderName = bindingCfDialog.etNewFolderName.text.toString()
                if (newFolderName.isNotBlank()) {
                    callback.invoke(newFolderName)
                }

            }
            btnCancel.setOnClickListener {
                hideCreateFolderDialog()
            }
        }
        mCreateFolderDialog.show()
    }

    private fun hideCreateFolderDialog(){
        this.mCreateFolderDialog.hide()
    }
}
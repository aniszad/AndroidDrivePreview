package com.az.googledrivelibraryxml.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.az.googledrivelibraryxml.databinding.CreateFolderDialogLayoutBinding

class CreateFileDialog(private val context: Context) {
    private lateinit var mCreateFolderDialog : Dialog
    private lateinit var binding : CreateFolderDialogLayoutBinding

    init {
    }
    fun showCreateFolderDialog(callback : (folderName : String) -> Unit){
        mCreateFolderDialog = Dialog(context)
        binding = CreateFolderDialogLayoutBinding.inflate(LayoutInflater.from(context))
        mCreateFolderDialog.setCancelable(true)
        mCreateFolderDialog.setCanceledOnTouchOutside(true)
        mCreateFolderDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        mCreateFolderDialog.setContentView(binding.root)
        binding.apply {
            btnCreateFolder.setOnClickListener {
                showLoadingButton()
                val newFolderName = binding.etNewFolderName.text.toString()
                if (newFolderName.isNotBlank()) {
                    callback.invoke(newFolderName)
                }

            }
            btnCancel.setOnClickListener {
                hideCreateFolderDialog()
            }
        }
        binding.piBtnCreateFolder.visibility = View.GONE
        binding.btnCreateFolder.text  = buildString{
            append("Create folder")
        }
        mCreateFolderDialog.show()
    }

    private fun showLoadingButton() {
        binding.piBtnCreateFolder.visibility = View.VISIBLE
        binding.btnCreateFolder.text  = ""
    }

    fun hideCreateFolderDialog(){
        this.mCreateFolderDialog.hide()
    }

}
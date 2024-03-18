package com.az.drivepreviewlibrary.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.az.drivepreviewlibrary.databinding.CreateFolderDialogLayoutBinding

/**
 * Create file dialog
 *
 * @property context
 * @constructor Create empty Create file dialog
 */
class CreateFileDialog(private val context: Context) {
    private lateinit var mCreateFolderDialog : Dialog
    private lateinit var binding : CreateFolderDialogLayoutBinding

    init {
    }

    /**
     * Show create folder dialog
     *
     * @param callback
     * @receiver
     */
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
        binding.btnCreateFolder.text  = buildString{
            append("Create folder")
        }
        mCreateFolderDialog.show()
    }

    private fun showLoadingButton() {
        binding.piLoginLoad.visibility = View.VISIBLE
        binding.btnCreateFolder.text  = ""
    }

    /**
     * Hide create folder dialog
     *
     */
    fun hideCreateFolderDialog(){
        this.mCreateFolderDialog.hide()
    }

}
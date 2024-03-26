package com.az.androiddrivepreview.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.az.androiddrivepreview.databinding.CreateFolderDialogLayoutBinding
import com.az.androiddrivepreview.databinding.UploadFileDialogLayoutBinding

/**
 * Dialogs
 *
 * @property context
 * @constructor Create empty Create file dialog
 */
class Dialogs(private val context: Context) {
    private lateinit var mCreateFolderDialog : Dialog
    private lateinit var mUploadFileDialog : Dialog
    private lateinit var createFolderBinding : CreateFolderDialogLayoutBinding
    private lateinit var uploadFileBinding : UploadFileDialogLayoutBinding

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
        createFolderBinding = CreateFolderDialogLayoutBinding.inflate(LayoutInflater.from(context))
        mCreateFolderDialog.setCancelable(true)
        mCreateFolderDialog.setCanceledOnTouchOutside(true)
        mCreateFolderDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        mCreateFolderDialog.setContentView(createFolderBinding.root)
        createFolderBinding.apply {
            btnCreateFolder.setOnClickListener {
                showLoadingButton()
                val newFolderName = createFolderBinding.etNewFolderName.text.toString()
                if (newFolderName.isNotBlank()) {
                    callback.invoke(newFolderName)
                }

            }
            btnCancel.setOnClickListener {
                hideCreateFolderDialog()
            }
        }
        createFolderBinding.btnCreateFolder.text  = buildString{
            append("Create folder")
        }
        mCreateFolderDialog.show()
    }

    private fun showLoadingButton() {
        createFolderBinding.piLoginLoad.visibility = View.VISIBLE
        createFolderBinding.btnCreateFolder.text  = ""
    }

    /**
     * Hide create folder dialog
     *
     */
    fun hideCreateFolderDialog(){
        this.mCreateFolderDialog.hide()
    }

    fun showUploadFileDialog(fileName:String, callback : () -> Unit){
        mUploadFileDialog = Dialog(context)
        uploadFileBinding = UploadFileDialogLayoutBinding.inflate(LayoutInflater.from(context))
        mUploadFileDialog.setCancelable(true)
        mUploadFileDialog.setCanceledOnTouchOutside(true)
        mUploadFileDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        mUploadFileDialog.setContentView(uploadFileBinding.root)
        uploadFileBinding.etFileName.setText(fileName)

        uploadFileBinding.apply {

            btnUploadFile.setOnClickListener {
                showUploadLoadingButton()
                callback.invoke()
            }
            btnCancel.setOnClickListener {
                hideUploadFileDialog(null)
            }
        }
        mUploadFileDialog.show()
    }
    private fun showUploadLoadingButton() {
        uploadFileBinding.piLoginLoad.visibility = View.VISIBLE
        uploadFileBinding.btnUploadFile.text  = ""
    }

    fun hideUploadFileDialog(toastMessage : String?){
        this.mUploadFileDialog.hide()
        if (toastMessage != null) Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
    }

}
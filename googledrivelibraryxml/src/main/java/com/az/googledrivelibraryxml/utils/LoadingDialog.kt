package com.az.googledrivelibraryxml.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import com.az.googledrivelibraryxml.databinding.CreateFolderDialogLayoutBinding
import com.az.googledrivelibraryxml.databinding.LoadingBarLayoutBinding

/**
 * Loading dialog
 *
 * @property context
 * @constructor Create empty Loading dialog
 */
class LoadingDialog(private val context: Context) {
    private lateinit var loadingDialog : Dialog

    /**
     * Show create folder dialog
     *
     */
    fun showLoadingDialog(){
        loadingDialog = Dialog(context)
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val bindingCfDialog = LoadingBarLayoutBinding.inflate(LayoutInflater.from(context))
        loadingDialog.setContentView(bindingCfDialog.root)
        loadingDialog.show()
    }

    fun hideLoadingDialog(){
        this.loadingDialog.hide()
    }
}
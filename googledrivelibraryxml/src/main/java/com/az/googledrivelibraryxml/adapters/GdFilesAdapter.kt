package com.az.googledrivelibraryxml.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.az.googledrivelibraryxml.R
import com.az.googledrivelibraryxml.databinding.GoogleDriveItemLayoutBinding
import com.az.googledrivelibraryxml.models.FileDriveItem
import com.az.googledrivelibraryxml.models.ItemType
import com.az.googledrivelibraryxml.models.Permissions
import kotlin.math.pow

class GdFilesAdapter(
    private val context: Context,
    private var filesList : List<FileDriveItem>,
    private val permissions : List<Permissions>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var fileOptions : FileOptions
    private lateinit var accessFileListener : AccessFileListener
    inner class FileViewHolder(binding: GoogleDriveItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        val iconFileType = binding.imFileType
        val btnMore = binding.btnMore
        val tvFileName = binding.tvFileName
        val tvFileSize = binding.tvFileSize
        val tvLastEdited = binding.tvLastEdited
        val root = binding.root
    }

    interface AccessFileListener{
        fun onOpenFile(webContentLink: String)
        fun onOpenFolder(folderId: String, folderName : String)

    }
    fun setAccessFileListener(accessFileListener: AccessFileListener){
        this.accessFileListener = accessFileListener
    }
    interface FileOptions{

        fun onDownload(fileId: String)
        fun onShare(webViewLink: String)
        fun onDelete(fileId:String)
    }

    fun setFileOptionsInterface(fileOptions: FileOptions){
        this.fileOptions = fileOptions
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return FileViewHolder(GoogleDriveItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return filesList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = filesList[position]
        when(holder){
            is FileViewHolder ->{
                holder.apply {
                    tvFileName.text = currentItem.fileName
                    tvFileSize.text = formatSize(currentItem.size)
                    tvLastEdited.text = currentItem.lastModified
                    iconFileType.setImageDrawable(setFileIcon(currentItem.fileType))
                    btnMore.setOnClickListener {
                        showOptionsMenu(it,currentItem)
                    }
                    root.setOnClickListener {
                        if (currentItem.fileType != ItemType.FOLDER){
                            accessFileListener.onOpenFile(currentItem.webViewLink)
                        }else{
                            accessFileListener.onOpenFolder(currentItem.fileId, folderName = currentItem.fileName)
                        }
                    }

                }
            }
        }

    }

    fun updateData(files: List<FileDriveItem>) {
        this.filesList = files
        notifyDataSetChanged()
    }

    // Non adapter functions
    private fun showOptionsMenu(anchorView: View, currentItem: FileDriveItem) {
        val popupMenu = PopupMenu(context, anchorView)
        popupMenu.menuInflater.inflate(R.menu.file_options_menu, popupMenu.menu)
        popupMenu.menu.findItem(R.id.btn_download).isVisible = permissions.contains(Permissions.USER) || permissions.contains(Permissions.ADMIN)
        popupMenu.menu.findItem(R.id.btn_share).isVisible = permissions.contains(Permissions.USER) || permissions.contains(Permissions.ADMIN)
        popupMenu.menu.findItem(R.id.btn_delete).isVisible = permissions.contains(Permissions.ADMIN)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.btn_download -> {
                    fileOptions.onDownload(currentItem.downloadUrl)
                    true
                }
                R.id.btn_share -> {
                    fileOptions.onShare(currentItem.webViewLink)
                    true
                }
                R.id.btn_delete -> {
                    fileOptions.onDelete(currentItem.fileId)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }
    private fun formatSize(size: Long): String {

        return when{
            size == 0L ->{""}

            size/1000.0.pow(3.0) >= 1000 ->{
                "${size/ 1000.0.pow(3.0)} GB"
            }
            (size/1000.0.pow(2.0) >= 1000) ->{
                "${size/1000.0.pow(2.0)} MB"
            }
            size/1000 < 1000 ->{
                "${size/1000} KB"
            }
            else ->{""}
        }
    }

    private fun setFileIcon(fileType: ItemType): Drawable? {
        // TODO -- returning a convenient icon for each file type
        return null
    }

}
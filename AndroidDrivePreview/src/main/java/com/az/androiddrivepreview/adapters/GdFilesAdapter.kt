package com.az.androiddrivepreview.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.az.androiddrivepreview.R
import com.az.androiddrivepreview.databinding.EmptyLayoutItemBinding
import com.az.androiddrivepreview.databinding.GoogleDriveItemLayoutBinding
import com.az.androiddrivepreview.databinding.LoadingBarLayoutBinding
import com.az.androiddrivepreview.databinding.NoDataLayoutItemBinding

import com.az.androiddrivepreview.data.models.FileDriveItem
import com.az.androiddrivepreview.data.models.ItemType
import com.az.androiddrivepreview.utils.CustomDateFormatter
import com.az.androiddrivepreview.utils.FileDetailsAdapter
import com.az.androiddrivepreview.utils.Permissions

class GdFilesAdapter(
    private val context: Context,
    private var filesList : List<FileDriveItem>,
    private val permissions : List<Permissions>,
    private var filePathCopyable : Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var fileOptions : FileOptions
    private lateinit var accessFileListener : AccessFileListener
    private lateinit var accessFolderListener: AccessFolderListener
    private var isLoading = true
    private var customDateFormatter = CustomDateFormatter()
    private val fileDetailsAdapter = FileDetailsAdapter()

    inner class FileViewHolder(binding: GoogleDriveItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        val iconFileType = binding.imFileType
        val btnMore = binding.btnMore
        val btnCopy = binding.btnCopy
        val tvFileName = binding.tvFileName
        val tvFileSize = binding.tvFileSize
        val tvCreationDate = binding.tvCreationDate
        val root = binding.root
    }
    inner class LoadingBarViewHolder(binding : LoadingBarLayoutBinding) : RecyclerView.ViewHolder(binding.root)
    inner class NoDataViewHolder(binding : NoDataLayoutItemBinding) : RecyclerView.ViewHolder(binding.root)
    inner class EmptyElementViewHolder(binding : EmptyLayoutItemBinding) : RecyclerView.ViewHolder(binding.root)

    interface AccessFolderListener{
        fun onOpenFolder(folderId: String, folderName : String)
    }
    fun setAccessFolderListener(accessFolderListener: AccessFolderListener) {
        this.accessFolderListener = accessFolderListener
    }
    interface AccessFileListener{
        fun onOpenFile(webContentLink: String)
    }
    fun setAccessFileListener(accessFileListener: AccessFileListener){
        this.accessFileListener = accessFileListener
    }
    interface FileOptions{

        fun onDownload(fileId: String, fileName:String)
        fun onShare(webViewLink: String)
        fun onDelete(fileId:String)
        fun copyFilePath(fileName: String)
    }
    fun setFileOptionsInterface(fileOptions: FileOptions){
        this.fileOptions = fileOptions
    }
    override fun getItemViewType(position: Int): Int {
        Log.e("list content", filesList.toString())
        return when{
            isLoading -> LOADING_VIEW_TYPE
            filesList.isEmpty() -> NO_DATA_VIEW_TYPE
            position == filesList.size -> EMPTY_ITEM_VIEW_TYPE
            else -> DATA_VIEW_TYPE
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType){
            DATA_VIEW_TYPE -> {
                FileViewHolder(GoogleDriveItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            EMPTY_ITEM_VIEW_TYPE -> {
                EmptyElementViewHolder(EmptyLayoutItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            NO_DATA_VIEW_TYPE -> {
                NoDataViewHolder(NoDataLayoutItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                )
            }
            LOADING_VIEW_TYPE ->{
                LoadingBarViewHolder(LoadingBarLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
                )
                )
            }

            else -> {
                LoadingBarViewHolder(LoadingBarLayoutBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                )
            }
        }
    }
    override fun getItemCount(): Int {
        return if (this.filesList.isEmpty()){
            1
        }else{
            filesList.size+1
        }
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FileViewHolder) {
            val currentItem = filesList[position]
            val isFolder = fileDetailsAdapter.fileOrDirectory(currentItem.mimeType) == ItemType.FOLDER

            with(holder) {
                tvFileName.text = currentItem.fileName
                tvCreationDate.text = customDateFormatter.formatDate(currentItem.createdDate)
                iconFileType.setImageDrawable(
                    fileDetailsAdapter.getIconFromMimeType(context, currentItem.mimeType)
                )

                btnMore.visibility = if (isFolder) View.GONE else View.VISIBLE
                btnCopy.visibility = if (isFolder || !filePathCopyable) View.GONE else View.VISIBLE
                tvFileSize.text = if (isFolder) "" else fileDetailsAdapter.formatSize(currentItem.size)

                btnMore.setOnClickListener { showMorePopupMenu(it, currentItem) }
                btnCopy.setOnClickListener { fileOptions.copyFilePath(currentItem.fileName) }
                root.setOnClickListener {
                    if (isFolder) {
                        accessFolderListener.onOpenFolder(currentItem.fileId, folderName = currentItem.fileName)
                    } else {
                        accessFileListener.onOpenFile(currentItem.webViewLink)
                    }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(files: List<FileDriveItem>) {
        this.filesList = files
        notifyDataSetChanged()
    }

    private fun showMorePopupMenu(anchorView: View, currentItem: FileDriveItem) {
        val popupMenu = PopupMenu(context, anchorView)
        popupMenu.menuInflater.inflate(R.menu.file_options_menu, popupMenu.menu)
        popupMenu.menu.findItem(R.id.btn_download).isVisible = permissions.contains(Permissions.USER) || permissions.contains(
            Permissions.ADMIN)
        popupMenu.menu.findItem(R.id.btn_share).isVisible = permissions.contains(Permissions.USER) || permissions.contains(
            Permissions.ADMIN) || permissions.contains(Permissions.STRICT)
        popupMenu.menu.findItem(R.id.btn_delete).isVisible = permissions.contains(Permissions.ADMIN)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.btn_download -> {
                    fileOptions.onDownload(currentItem.fileId, currentItem.fileName)
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

    @SuppressLint("NotifyDataSetChanged")
    fun showLoading() {
        this.filesList = emptyList()
        isLoading = true
        notifyDataSetChanged()
    }
    fun hideLoading() {
        isLoading = false
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateFilePathCopyable(filePathCopyable : Boolean){
        this.filePathCopyable = filePathCopyable
        notifyDataSetChanged()
    }

    companion object{
        const val EMPTY_ITEM_VIEW_TYPE = 3
        const val NO_DATA_VIEW_TYPE = 2
        const val DATA_VIEW_TYPE = 1
        const val LOADING_VIEW_TYPE = 0
    }
}
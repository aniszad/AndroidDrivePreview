package com.az.googledrivelibraryxml.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.az.googledrivelibraryxml.R
import com.az.googledrivelibraryxml.databinding.EmptyLayoutItemBinding
import com.az.googledrivelibraryxml.databinding.GoogleDriveItemLayoutBinding
import com.az.googledrivelibraryxml.databinding.LoadingBarLayoutBinding
import com.az.googledrivelibraryxml.databinding.NoDataLayoutItemBinding
import com.az.googledrivelibraryxml.models.FileDriveItem
import com.az.googledrivelibraryxml.models.ItemType
import com.az.googledrivelibraryxml.utils.Permissions
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.pow

class GdFilesAdapter(
    private val context: Context,
    private var filesList : List<FileDriveItem>,
    private val permissions : List<Permissions>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var fileOptions : FileOptions
    private lateinit var accessFileListener : AccessFileListener
    private lateinit var accessFolderListener: AccessFolderListener
    private var isLoading = true
    inner class FileViewHolder(binding: GoogleDriveItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        val iconFileType = binding.imFileType
        val btnMore = binding.btnMore
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
            LOADING_VIEW_TYPE->{
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
        when(holder){
            is FileViewHolder ->{
                val currentItem = filesList[position]
                holder.apply {
                    if (fileOrDirectory(currentItem.mimeType) == ItemType.FOLDER){
                        tvFileName.text = currentItem.fileName
                        btnMore.visibility = View.GONE
                        tvFileSize.text = ""
                        tvCreationDate.text = formatDate(currentItem.lastModified)
                        iconFileType.setImageDrawable(ContextCompat.getDrawable(
                            context,
                            getIconFromMimeType(currentItem.mimeType)
                        ))
                        btnMore.setOnClickListener {
                            showOptionsMenu(it,currentItem)
                        }
                        root.setOnClickListener {
                            accessFolderListener.onOpenFolder(currentItem.fileId, folderName = currentItem.fileName)
                        }
                    }else{
                        tvFileName.text = currentItem.fileName
                        tvFileSize.text = formatSize(currentItem.size)
                        tvCreationDate.text = formatDate(currentItem.lastModified)
                        iconFileType.setImageDrawable(ContextCompat.getDrawable(
                            context,
                            getIconFromMimeType(currentItem.mimeType)
                        ))
                        btnMore.setOnClickListener {
                            showOptionsMenu(it,currentItem)
                        }
                        root.setOnClickListener {
                            accessFileListener.onOpenFile(currentItem.webViewLink)
                        }
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
    // Non adapter functions
    private fun showOptionsMenu(anchorView: View, currentItem: FileDriveItem) {
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
    private fun getIconFromMimeType(mimeType: String): Int {
        if (fileOrDirectory(mimeType) == ItemType.FOLDER) {
            return R.drawable.icon_folder
        }
        return when {
            mimeType.startsWith("image/") -> {
                when (mimeType) {
                    "image/gif" -> R.drawable.icon_gif
                    "image/jpeg" -> R.drawable.icon_jpg
                    "image/png" -> R.drawable.icon_png
                    "image/svg+xml" -> R.drawable.icon_svg
                    else -> R.drawable.icon_img // For other image types
                }
            }
            mimeType == "application/pdf" -> R.drawable.icon_pdf
            mimeType == "audio/mpeg" -> R.drawable.icon_mp3
            mimeType == "video/x-msvideo" -> R.drawable.icon_avi
            mimeType == "video/x-matroska" -> R.drawable.icon_mkv
            mimeType == "application/vnd.ms-powerpoint" -> R.drawable.icon_ppt
            mimeType == "application/vnd.ms-excel" -> R.drawable.icon_xls
            mimeType == "application/zip" -> R.drawable.icon_zip
            mimeType == "image/vnd.adobe.photoshop" -> R.drawable.icon_psd
            mimeType == "text/plain" -> R.drawable.icon_txt
            mimeType == "application/illustrator" -> R.drawable.icon_ai
            mimeType == "application/msword" -> R.drawable.icon_doc
            mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> R.drawable.icon_doc
            mimeType == "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> R.drawable.icon_ppt
            mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> R.drawable.icon_xls
            mimeType == "application/json" -> R.drawable.icon_json
            mimeType == "text/csv" -> R.drawable.icon_csv
            mimeType == "application/x-rar-compressed" -> R.drawable.icon_rar // Adding RAR icon
            mimeType == "application/x-zip-compressed" -> R.drawable.icon_zip // Adding RAR icon
            else -> R.drawable.icon_other // Replace with a default icon
        }
    }


    private fun formatDate(inputDateString: String): String{
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = inputFormat.parse(inputDateString)
        return date?.let { outputFormat.format(it) } ?: ""
    }

    private fun fileOrDirectory(mimeType: String): ItemType {
        return if (mimeType == "application/vnd.google-apps.folder"){
            ItemType.FOLDER
        }else{
            ItemType.FILE
        }
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

    companion object{
        const val EMPTY_ITEM_VIEW_TYPE = 3
        const val NO_DATA_VIEW_TYPE = 2
        const val DATA_VIEW_TYPE = 1
        const val LOADING_VIEW_TYPE = 0
    }
}
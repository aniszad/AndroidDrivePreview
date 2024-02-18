package com.az.googledrivelibraryxml.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.az.googledrivelibraryxml.R
import com.az.googledrivelibraryxml.databinding.GoogleDriveItemLayoutBinding
import com.az.googledrivelibraryxml.databinding.LoadingBarLayoutBinding
import com.az.googledrivelibraryxml.models.FileDriveItem
import com.az.googledrivelibraryxml.models.ItemType
import com.az.googledrivelibraryxml.models.Permissions
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

    interface AccessFileListener{
        fun onOpenFile(webContentLink: String)
    }
    interface AccessFolderListener{
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
    override fun getItemViewType(position: Int): Int {
        return if(isLoading){
            NO_DATA_VIEW_TYPE
        }else{
            DATA_VIEW_TYPE
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType){
            NO_DATA_VIEW_TYPE->{
                LoadingBarViewHolder(LoadingBarLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
                )
                )
            }
            DATA_VIEW_TYPE -> {
                FileViewHolder(GoogleDriveItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))
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
            filesList.size
        }
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is FileViewHolder ->{
                val currentItem = filesList[position]
                holder.apply {

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
                        if (fileOrDirectory(currentItem.mimeType) != ItemType.FOLDER){
                            accessFileListener.onOpenFile(currentItem.webViewLink)
                        }else{
                            accessFolderListener.onOpenFolder(currentItem.fileId, folderName = currentItem.fileName)
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
    private fun getIconFromMimeType(mimeType: String): Int {
        if (fileOrDirectory(mimeType) == ItemType.FOLDER){
            return R.drawable.icon_folder
        }
        return when (mimeType) {
            "application/pdf" -> R.drawable.icon_pdf
            "image/gif" -> R.drawable.icon_gif
            "audio/mpeg" -> R.drawable.icon_mp3
            "video/x-msvideo" -> R.drawable.icon_avi
            "video/x-matroska" -> R.drawable.icon_mkv
            "application/vnd.ms-powerpoint" -> R.drawable.icon_ppt
            "application/vnd.ms-excel" -> R.drawable.icon_xls
            "application/zip" -> R.drawable.icon_zip
            "image/vnd.adobe.photoshop" -> R.drawable.icon_psd
            "text/plain" -> R.drawable.icon_txt
            "application/illustrator" -> R.drawable.icon_ai
            "image/jpeg" -> R.drawable.icon_jpg // Added JPEG image type
            "image/png" -> R.drawable.icon_png // Added PNG image type
            else -> R.drawable.icon_other // Replace with a default icon
        }
    }
    fun formatDate(inputDateString: String): String{

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
    fun showLoading() {
        this.filesList = emptyList()
        isLoading = true
        notifyDataSetChanged()
    }
    fun hideLoading() {
        isLoading = false
        notifyDataSetChanged()
    }
    companion object{
        const val DATA_VIEW_TYPE = 1
        const val NO_DATA_VIEW_TYPE = 0
    }
}
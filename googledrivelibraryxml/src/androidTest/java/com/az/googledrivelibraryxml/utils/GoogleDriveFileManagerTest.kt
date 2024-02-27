package com.az.googledrivelibraryxml.utils

import android.content.Context
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.az.googledrivelibraryxml.adapters.GdFilesAdapter
import com.az.googledrivelibraryxml.api.GoogleDriveApi
import com.az.googledrivelibraryxml.models.FileDriveItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class GoogleDriveFileManagerTest {

    @Mock
    lateinit var context: Context
    @Mock
    lateinit var recyclerView: RecyclerView
    @Mock
    lateinit var textView: TextView
    @Mock
    lateinit var toolbar: Toolbar
    @Mock
    lateinit var lifecycleCoroutineScope: LifecycleCoroutineScope
    @Mock
    lateinit var googleDriveApi: GoogleDriveApi
    @Mock
    lateinit var gdFilesAdapter: GdFilesAdapter

    private lateinit var googleDriveFileManager: GoogleDriveFileManager

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this) // Initialize mocks

        `when`(recyclerView.adapter).thenReturn(gdFilesAdapter) // Mock RecyclerView's adapter

        googleDriveFileManager = GoogleDriveFileManager(
            context,
            "rootId",
            lifecycleCoroutineScope,
            Permissions.USER, // Set permissions as needed
            "jsonCredentialsPath",
            "applicationName"
        )
        googleDriveFileManager.setRecyclerView(recyclerView)
        googleDriveFileManager.setPathTextView(textView)
        googleDriveFileManager.setActionBar(toolbar)
        googleDriveFileManager.setAccessFileListener(googleDriveFileManager)
    }

    @Test
    fun testGetFiles() {
        // Mock API response
        val files = emptyList<FileDriveItem>()
        runBlocking {
            `when`(googleDriveApi.getDriveFiles(any())).thenReturn(files)

            // Call method
            googleDriveFileManager.getFiles("rootId")

            // Verify that the adapter is updated with the fetched files
            verify(recyclerView).adapter = any()

            // Assert that the RecyclerView adapter is updated
            assertTrue("RecyclerView adapter should be updated", recyclerView.adapter != null)
        }
    }
    @Test
    fun testQueryFiles() {
        // Mock API response
        val query = "exampleQuery"
        val files = emptyList<FileDriveItem>()
        runBlocking {
            `when`(googleDriveApi.queryDriveFiles(any(), eq(query))).thenReturn(files)

            // Call method
            googleDriveFileManager.queryFiles(query)

            // Verify that the adapter is updated with the fetched files
            verify(gdFilesAdapter).updateData(files)
        }

        // Verify that showLoading() and hideLoading() are called on the adapter
        verify(gdFilesAdapter).showLoading()
        verify(gdFilesAdapter).hideLoading()
    }
}

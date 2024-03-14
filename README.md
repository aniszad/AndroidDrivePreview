# Google Drive File Manager Android Library
The Google Drive File Manager Android Library is a simple and convenient way to integrate Google Drive file management functionalities into your Android applications. With this library, you can easily browse, upload, and manage files stored in Google Drive.

## Features
Browse files and folders stored in Google Drive
Upload files to Google Drive
Customizable toolbar for easy navigation and actions
Support for displaying file path and copying it to clipboard
Integration with RecyclerView for displaying files

## Installation
To integrate the Google Drive File Manager into your Android project, follow these steps:

Add the library to your project's dependencies.

gradle
Copy code
implementation 'com.example:googledrivefilemanager:1.0.0'
Make sure you have the necessary permissions and dependencies set up in your project.

## Usage
Here's a simple example of how to use the library in your Android application:

kotlin
Copy code
// Initialize Google Drive File Manager
val gdm = GoogleDriveFileManager(
    context, // Context
    lifecycleScope, // Lifecycle scope for launching coroutines
    Permissions.ADMIN, // User permissions
    CredentialsProvider(), // Credentials provider
    "test" // Application name
)

// Configure Google Drive File Manager
gdm.setRecyclerView(recyclerView) // Set RecyclerView to display files
   .setActionBar(toolbar) // Set ActionBar to display file name and actions
   .setRootFileId("1ZEmBUIPWUXr_nae82N7qQHudIFwaxRe5") // Set root file ID
   .setRootFileName("Files Bank") // Set root file name
   .activateNavigationPath(false) // Disable navigation path display
   .setFilePathCopyable(true) // Enable copying of file paths
   .setFilePickerListener(this@MainActivity) // Set file picker listener
   .initialize() // Initialize Google Drive File Manager
For more detailed usage instructions and customization options, refer to the documentation.

Contributing
Contributions are welcome! Please feel free to submit issues and pull requests.

License
This library is licensed under the MIT License.


package com.example.gphotosshare.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import com.example.gphotosshare.data.FileModel
import com.example.gphotosshare.data.FileRepository
import com.example.gphotosshare.ui.components.FileGridItem
import com.example.gphotosshare.ui.components.FileListItem
import com.example.gphotosshare.utils.StorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    repository: FileRepository,
    initialPath: String, // Kept for initialization if needed, but we rely on currentPath
    currentPath: String,
    onPathChange: (String) -> Unit,
    targetAppPackageName: String?,
    onSettingsClick: () -> Unit
) {
    // var currentPath by remember { mutableStateOf(initialPath) } // Hoisted
    var files by remember { mutableStateOf(emptyList<FileModel>()) }
    var selectedFiles = remember { mutableStateListOf<FileModel>() }
    var isGridView by remember { mutableStateOf(false) }
    var showLowSpaceDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    // val scope = rememberCoroutineScope() // Unused

    // Load files when path changes
    LaunchedEffect(currentPath) {
        withContext(Dispatchers.IO) {
            files = repository.listFiles(currentPath)
        }
    }

    // Handle Back Press to go up directory
    val isAtRoot = File(currentPath).absolutePath == File(repository.getDefaultPath()).absolutePath
    BackHandler(enabled = !isAtRoot) {
        val parent = File(currentPath).parent
        if (parent != null) {
            onPathChange(parent)
        }
    }

    fun handleFileClick(file: FileModel) {
        if (file.isDirectory) {
            onPathChange(file.path)
        } else {
            // Toggle selection
            val index = selectedFiles.indexOfFirst { it.path == file.path }
            if (index != -1) {
                selectedFiles.removeAt(index)
            } else {
                selectedFiles.add(file)
            }
        }
    }

    fun onProceedClick() {
        val totalSize = selectedFiles.sumOf { it.size }
        val availableSpace = StorageUtils.getAvailableStorage() // Internal storage check

        if (availableSpace < totalSize) {
            showLowSpaceDialog = true
        } else {
            shareFiles(context, selectedFiles, targetAppPackageName)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = File(currentPath).name.ifEmpty { "Internal Storage" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (File(currentPath).absolutePath != File(repository.getDefaultPath()).absolutePath) {
                        IconButton(onClick = {
                            val parent = File(currentPath).parent
                            if (parent != null) onPathChange(parent)
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.List else Icons.Default.GridView,
                            contentDescription = "Toggle View"
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedFiles.isNotEmpty()) {
                FloatingActionButton(onClick = { onProceedClick() }) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Next (${selectedFiles.size})")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No files found", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                 if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(files) { file ->
                            // Check selection state dynamically
                            val isSelected = selectedFiles.any { it.path == file.path }
                            FileGridItem(
                                file = file.copy(isSelected = isSelected),
                                onClick = { handleFileClick(file) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(files) { file ->
                             val isSelected = selectedFiles.any { it.path == file.path }
                            FileListItem(
                                file = file.copy(isSelected = isSelected),
                                onClick = { handleFileClick(file) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLowSpaceDialog) {
        AlertDialog(
            onDismissRequest = { showLowSpaceDialog = false },
            title = { Text("Low Storage Space") },
            text = { Text("There is not enough space left on the device and upload might fail.\n\nProceed anyway?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLowSpaceDialog = false
                        shareFiles(context, selectedFiles, targetAppPackageName)
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLowSpaceDialog = false }) {
                    Text("No")
                }
            }
        )
    }
}

fun shareFiles(context: android.content.Context, files: List<FileModel>, targetAppPackageName: String?) {
    // Generate Uri using FileProvider would be best practice, but for internal storage / sdcard
    // usually we need a FileProvider. 
    // However, the prompt implies "browse their device's entire internal storage".
    // If we are sharing to Google Photos, we should use FileProvider to be safe on Android N+.
    // But initializing FileProvider requires manifest and xml setup.
    // I will try to use simple Uri.fromFile first if targetSdk < 24 but wait targetSdk is 34.
    // MUST USE FILEPROVIDER.
    // I need to add FileProvider to manifest and create provider_paths.xml.
    // But wait, "Permission: MANAGE_EXTERNAL_STORAGE" gives us raw file access.
    // Sharing to other apps requires granting them read uri permission.
    
    // For simplicity in this generated code without complex fileprovider setup which varies by path:
    // I will implement a basic sharing intent. 
    // CRITICAL: Android 7.0+ throws FileUriExposedException. We MUST use FileProvider.
    // So I need to add that step to the plan if I haven't.
    // But since I'm in the middle of executing, I'll add the FileProvider setup now.
    
    // Actually, let's see if I can do it without FileProvider if I'm "system" or root?
    // No, I'm a normal app.
    // Okay, I will add FileProvider support.
    
    if (files.isEmpty()) return

    val uris = ArrayList<Uri>()
    // We need a helper to get URI.
    // For now, I'll put a placeholder TODO or try to implement FileProvider on the fly.
    // I'll assume we can use `androidx.core.content.FileProvider`.
    
    try {
        files.forEach { fileModel ->
             val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                fileModel.file
            )
            uris.add(uri)
        }

        val intent = Intent().apply {
            if (uris.size == 1) {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uris[0])
            } else {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
            type = "*/*" // Or specific mime type like "image/*"
            // Set mime type based on first file?
            val mimeType = if (files.any { it.extension in setOf("mp4", "mkv", "webm", "avi") }) "video/*" else "image/*"
            type = mimeType
            
            setPackage(targetAppPackageName ?: "com.google.android.apps.photos")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        // Check if package exists
        val packageManager = context.packageManager
        if (intent.resolveActivity(packageManager) != null) {
            context.startActivity(intent)
        } else {
             Toast.makeText(context, "Google Photos not installed", Toast.LENGTH_SHORT).show()
             // Fallback to generic chooser
             intent.setPackage(null)
            context.startActivity(Intent.createChooser(intent, "Share Media"))
        }
    } catch (e: Exception) {
         Toast.makeText(context, "Error preparing share: ${e.message}", Toast.LENGTH_LONG).show()
         e.printStackTrace()
    }
}

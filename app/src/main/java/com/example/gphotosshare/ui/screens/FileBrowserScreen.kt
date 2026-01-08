package com.example.gphotosshare.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    repository: FileRepository,
    initialPath: String, // Kept for initialization if needed, but we rely on currentPath
    currentPath: String,
    onPathChange: (String) -> Unit,
    selectedFiles: MutableList<FileModel>, 
    targetAppPackageName: String?,
    keepSelection: Boolean,
    showThumbnails: Boolean,
    onSettingsClick: () -> Unit
) {
    // var currentPath by remember { mutableStateOf(initialPath) } // Hoisted
    var files by remember { mutableStateOf(emptyList<FileModel>()) }
    // var selectedFiles = remember { mutableStateListOf<FileModel>() } // Hoisted
    var isGridView by remember { mutableStateOf(false) }
    var showLowSpaceDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    // val scope = rememberCoroutineScope() // Unused
    
    // Load badge icon if targetAppPackageName is set
    var targetAppIcon by remember { mutableStateOf<android.graphics.drawable.Drawable?>(null) }
    
    LaunchedEffect(targetAppPackageName) {
        if (targetAppPackageName != null) {
            withContext(Dispatchers.IO) {
                try {
                    // It might be "pkg/cls" or just "pkg"
                    if (targetAppPackageName.contains("/")) {
                        val split = targetAppPackageName.split("/")
                        val componentName = android.content.ComponentName(split[0], split[1])
                        targetAppIcon = context.packageManager.getActivityIcon(componentName)
                    } else {
                         targetAppIcon = context.packageManager.getApplicationIcon(targetAppPackageName)
                    }
                } catch (e: Exception) {
                    try {
                        val packageName = targetAppPackageName.substringBefore("/")
                        targetAppIcon = context.packageManager.getApplicationIcon(packageName)
                    } catch (e2: Exception) {
                         targetAppIcon = null
                    }
                }
            }
        } else {
            targetAppIcon = null
        }
    }

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
            if (!keepSelection) selectedFiles.clear()
            onPathChange(parent)
        }
    }

    fun handleFileClick(file: FileModel) {
        if (file.isDirectory) {
            if (!keepSelection) selectedFiles.clear()
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

    // Optimization: Create a Set of selected paths for O(1) lookup during rendering
    // selectedFiles is a SnapshotStateList, so this derivedState will update when it changes.
    val selectedPaths by remember { androidx.compose.runtime.derivedStateOf { selectedFiles.map { it.path }.toSet() } }

    // Hoist states for scrolling
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant, // Slightly lighter than background
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                title = {
                    val file = File(currentPath)
                    val titleText = if (file.absolutePath == File(repository.getDefaultPath()).absolutePath || file.name == "0") "Internal Storage" else file.name
                    Text(
                        text = titleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp) 
                    )
                },
                navigationIcon = {
                    if (File(currentPath).absolutePath != File(repository.getDefaultPath()).absolutePath) {
                        IconButton(onClick = {
                            val parent = File(currentPath).parent
                            if (parent != null) {
                                if (!keepSelection) selectedFiles.clear()
                                onPathChange(parent)
                            }
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
                androidx.compose.material3.ExtendedFloatingActionButton(
                    onClick = { onProceedClick() },
                    icon = { Icon(Icons.Default.Share, contentDescription = "Share") },
                    text = { 
                         Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Next (${selectedFiles.size})")
                            if (targetAppIcon != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Image(
                                    painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(drawable = targetAppIcon),
                                    contentDescription = "Target App",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                )
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
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            count = files.size,
                            key = { index -> files[index].path }
                        ) { index ->
                            val file = files[index]
                            val isSelected = file.path in selectedPaths
                            FileGridItem(
                                file = file.copy(isSelected = isSelected),
                                showThumbnail = showThumbnails,
                                onClick = { handleFileClick(file) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 24.dp), // Extra padding for fast scroll
                        modifier = Modifier.fillMaxSize()
                    ) {
                         items(
                            count = files.size,
                            key = { index -> files[index].path }
                         ) { index ->
                            val file = files[index]
                            val isSelected = file.path in selectedPaths
                            FileListItem(
                                file = file.copy(isSelected = isSelected),
                                showThumbnail = showThumbnails,
                                onClick = { handleFileClick(file) }
                            )
                        }
                    }
                }

                // Fast Scroll Implementation
                FastScroller(
                    files = files,
                    onScrollTo = { index ->
                        coroutineScope.launch {
                            if (isGridView) {
                                gridState.scrollToItem(index)
                            } else {
                                listState.scrollToItem(index)
                            }
                        }
                    }
                )
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

@Composable
fun FastScroller(
    files: List<FileModel>,
    onScrollTo: (Int) -> Unit
) {
    if (files.size < 20) return // Only show scrollbar if list is scrollable (approx check)

    var isDragging by remember { mutableStateOf(false) }
    var currentLetter by remember { mutableStateOf<Char?>(null) }
    var currentY by remember { mutableStateOf(0f) }
    val listSize = files.size

    Box(modifier = Modifier.fillMaxSize()) {
        // Scroll Bar Track & Touch Area
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 2.dp)
                .width(24.dp) // Generous touch target
                .fillMaxHeight()
                .pointerInput(listSize) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            currentY = offset.y
                            // Initial tap also triggers scroll ?? Standard behavior is drag.
                            // But for fast access, tapping jump is nice. 
                            // detectVerticalDragGestures doesn't handle tap. 
                            // We can use detectTapGestures separately but drag is sufficient for "FastScroll".
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onVerticalDrag = { change, _ ->
                            currentY = change.position.y
                            // Proportional Logic
                            val progress = (currentY / size.height.toFloat()).coerceIn(0f, 1f)
                            val index = (progress * (listSize - 1)).toInt()
                            
                            val file = files.getOrNull(index)
                            if (file != null) {
                                currentLetter = file.name.firstOrNull()?.uppercaseChar() ?: '#'
                                onScrollTo(index)
                            }
                        }
                    )
                }
        ) {
              // Visual Indicator (Thin Bar)
              Box(
                  modifier = Modifier
                      .align(Alignment.Center)
                      .width(4.dp)
                      .fillMaxHeight(0.95f) // Slightly padded vertically
                      .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
              )
        }

        // Hint Bubble
        if (isDragging && currentLetter != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 100.dp, end = 50.dp) 
                    .offset(y = with(androidx.compose.ui.platform.LocalDensity.current) { currentY.toDp() - 80.dp })
                    .size(60.dp)
                    .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentLetter.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
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
            // Use FileProvider
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider", // Matches manifest
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
            // type = "*/*" // Or specific mime type like "image/*"
            // Set mime type based on first file?
            val mimeType = if (files.any { it.extension in setOf("mp4", "mkv", "webm", "avi") }) "video/*" else "image/*"
            type = mimeType
            
            if (targetAppPackageName != null) {
                if (targetAppPackageName.contains("/")) {
                    val split = targetAppPackageName.split("/")
                    if (split.size == 2) {
                        component = android.content.ComponentName(split[0], split[1])
                    } else {
                         setPackage(targetAppPackageName)
                    }
                } else {
                     setPackage(targetAppPackageName)
                }
            }
            // If null, we don't set package, allowing system chooser to resolve
            
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val shareIntent = if (targetAppPackageName == null) {
            Intent.createChooser(intent, "Share files")
        } else {
            intent
        }
        
        try {
            context.startActivity(shareIntent)
        } catch (e: android.content.ActivityNotFoundException) {
            android.widget.Toast.makeText(context, "Selected app not found or nothing available to share.", android.widget.Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
         android.widget.Toast.makeText(context, "Error preparing share: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
         e.printStackTrace()
    }
}

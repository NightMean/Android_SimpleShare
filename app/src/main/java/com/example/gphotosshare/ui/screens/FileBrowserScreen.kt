package com.example.gphotosshare.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.example.gphotosshare.data.FileModel
import com.example.gphotosshare.data.FileRepository
import com.example.gphotosshare.ui.components.FileGridItem
import com.example.gphotosshare.ui.components.FileListItem
import com.example.gphotosshare.utils.StorageUtils
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

enum class SortOption {
    NAME, SIZE, DATE, TYPE
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun FileBrowserScreen(
    repository: FileRepository,
    currentPath: String,
    onPathChange: (String) -> Unit,
    selectedFiles: MutableList<FileModel>, 
    targetAppPackageName: String?,
    keepSelection: Boolean,
    showThumbnails: Boolean,
    checkLowStorage: Boolean,
    onSettingsClick: () -> Unit
) {
    var rawFiles by remember { mutableStateOf(emptyList<FileModel>()) }
    var isGridView by remember { mutableStateOf(false) }
    var showLowSpaceDialog by remember { mutableStateOf(false) }

    // Logic States
    var sortOption by remember { mutableStateOf(SortOption.NAME) }
    var isSortAscending by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    
    // Load badge icon
    var targetAppIcon by remember { mutableStateOf<android.graphics.drawable.Drawable?>(null) }
    
    LaunchedEffect(targetAppPackageName) {
        if (targetAppPackageName != null) {
            withContext(Dispatchers.IO) {
                try {
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

    // Load files
    LaunchedEffect(currentPath) {
        withContext(Dispatchers.IO) {
            rawFiles = repository.listFiles(currentPath)
        }
    }
    
    fun refreshFiles() {
        rawFiles = repository.listFiles(currentPath)
    }

    // Filter and Sort Logic
    val displayedFiles by remember(rawFiles, searchQuery, sortOption, isSortAscending) {
        derivedStateOf {
            var result = if (searchQuery.isBlank()) {
                rawFiles
            } else {
                rawFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }

            result = when (sortOption) {
                SortOption.NAME -> if (isSortAscending) result.sortedBy { it.name.lowercase(Locale.getDefault()) } else result.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
                SortOption.SIZE -> if (isSortAscending) result.sortedBy { it.size } else result.sortedByDescending { it.size }
                SortOption.DATE -> if (isSortAscending) result.sortedBy { it.file.lastModified() } else result.sortedByDescending { it.file.lastModified() }
                SortOption.TYPE -> if (isSortAscending) result.sortedBy { it.extension } else result.sortedByDescending { it.extension }
            }
            // Always keep directories on top? User didn't specify, but typically standard.
            // Current repository logic puts folders on top. Let's preserve that preference if SortOption is NAME, but others might mix.
            // If user sorts by Size, they likely want big files on top regardless of folder.
            // Let's stick to pure sort for now as requested.
            result
        }
    }

    // Handle Back Press
    val isAtRoot = File(currentPath).absolutePath == File(repository.getDefaultPath()).absolutePath
    BackHandler(enabled = !isAtRoot || isSearchActive) {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        } else {
            val parent = File(currentPath).parent
            if (parent != null) {
                if (!keepSelection) selectedFiles.clear()
                onPathChange(parent)
            }
        }
    }

    fun handleFileClick(file: FileModel) {
        if (file.isDirectory) {
            if (!keepSelection) selectedFiles.clear()
            // Clear search on navigation
            isSearchActive = false
            searchQuery = ""
            onPathChange(file.path)
        } else {
            val index = selectedFiles.indexOfFirst { it.path == file.path }
            if (index != -1) {
                selectedFiles.removeAt(index)
            } else {
                selectedFiles.add(file)
            }
        }
    }

    fun onProceedClick() {
        if (checkLowStorage) {
            val totalSize = selectedFiles.sumOf { it.size }
            val availableSpace = StorageUtils.getAvailableStorage()

            if (availableSpace < totalSize) {
                showLowSpaceDialog = true
            } else {
                shareFiles(context, selectedFiles, targetAppPackageName)
            }
        } else {
            shareFiles(context, selectedFiles, targetAppPackageName)
        }
    }

    val selectedPaths by remember { derivedStateOf { selectedFiles.map { it.path }.toSet() } }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background, // Match app background
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    if (isSearchActive) {
                        val focusRequester = remember { FocusRequester() }
                        val keyboardController = LocalSoftwareKeyboardController.current

                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                        
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                        )
                    } else {
                        val file = File(currentPath)
                        val titleText = if (file.absolutePath == File(repository.getDefaultPath()).absolutePath || file.name == "0") "Internal Storage" else file.name
                        Text(
                            text = titleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = { 
                            isSearchActive = false 
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close Search")
                        }
                    } else if (File(currentPath).absolutePath != File(repository.getDefaultPath()).absolutePath) {
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
                    if (isSearchActive && searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    } else {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search
                    IconButton(onClick = { isSearchActive = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    
                    // View Toggle
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.List else Icons.Default.GridView,
                            contentDescription = "Toggle View"
                        )
                    }

                    // Select All
                    IconButton(onClick = {
                        if (displayedFiles.all { it.path in selectedPaths }) {
                             selectedFiles.removeAll { file -> displayedFiles.any { it.path == file.path && !it.isDirectory } }
                        } else {
                             // Only select files, often we don't select folders for sharing
                             val filesToSelect = displayedFiles.filter { !it.isDirectory }
                             // Add only distinct
                             filesToSelect.forEach { 
                                 if (selectedFiles.none { sel -> sel.path == it.path }) {
                                     selectedFiles.add(it)
                                 }
                             }
                        }
                    }) {
                        Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                    }
                    
                    // Refresh
                    IconButton(onClick = { refreshFiles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }

                    // Sort
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(option.name.lowercase().replaceFirstChar { it.uppercase() })
                                            if (sortOption == option) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    // In many apps: Arrow UP = Ascending (A->Z, 0->9). Arrow DOWN = Descending.
                                                    // Let's use ArrowUpward for Ascending.
                                                    imageVector = if (isSortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = { 
                                        if (sortOption == option) {
                                            isSortAscending = !isSortAscending
                                        } else {
                                            sortOption = option
                                            isSortAscending = true // Default to asc for new sort
                                        }
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
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
                                    painter = rememberDrawablePainter(drawable = targetAppIcon),
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
        Box(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)) { // Enforce background
            
            if (displayedFiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (isSearchActive) "No results found" else "No files found", style = MaterialTheme.typography.bodyLarge)
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
                            count = displayedFiles.size,
                            key = { index -> displayedFiles[index].path }
                        ) { index ->
                            val file = displayedFiles[index]
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
                        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                         items(
                            count = displayedFiles.size,
                            key = { index -> displayedFiles[index].path }
                         ) { index ->
                            val file = displayedFiles[index]
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
                val scrollStateValues = remember(isGridView, listState, gridState, displayedFiles) {
                    derivedStateOf {
                        val totalItems: Int
                        val visibleItemsCount: Int
                        val firstIndex: Int
                        val firstOffset: Int
                        val itemSize: Int
                        
                        if (isGridView) {
                            val layout = gridState.layoutInfo
                            totalItems = layout.totalItemsCount
                            val visibleInfo = layout.visibleItemsInfo
                            visibleItemsCount = visibleInfo.size
                            firstIndex = gridState.firstVisibleItemIndex
                            firstOffset = gridState.firstVisibleItemScrollOffset
                            itemSize = gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height ?: 0
                        } else {
                            val layout = listState.layoutInfo
                            totalItems = layout.totalItemsCount
                            val visibleInfo = layout.visibleItemsInfo
                            visibleItemsCount = visibleInfo.size
                            firstIndex = listState.firstVisibleItemIndex
                            firstOffset = listState.firstVisibleItemScrollOffset
                            itemSize = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
                        }
                        
                        if (totalItems == 0 || visibleItemsCount == 0 || itemSize <= 0) 0f to 0f
                        else {
                             val viewportHeight: Int = if (isGridView) gridState.layoutInfo.viewportSize.height else listState.layoutInfo.viewportSize.height
                             
                             val estimatedTotalContentHeight = itemSize.toFloat() * totalItems
                             val fraction = (viewportHeight.toFloat() / estimatedTotalContentHeight).coerceIn(0f, 1f)
                             val offsetFraction = firstOffset.toFloat() / itemSize.toFloat()
                             val effectiveIndex = firstIndex + offsetFraction
                             val maxIndex = (totalItems - visibleItemsCount).coerceAtLeast(1)
                             val progress = (effectiveIndex / maxIndex.toFloat()).coerceIn(0f, 1f)
                             progress to fraction
                        }
                    }
                }

                FastScroller(
                    files = displayedFiles,
                    scrollState = scrollStateValues,
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                    onScrollTo = { progress ->
                         coroutineScope.launch {
                            val totalItems = if (isGridView) {
                                gridState.layoutInfo.totalItemsCount
                            } else {
                                listState.layoutInfo.totalItemsCount
                            }

                            val itemSize = if (isGridView) {
                                gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height ?: 0
                            } else {
                                listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
                            }
                            
                            if (itemSize > 0) {
                                val totalPixels = totalItems * itemSize.toFloat()
                                val targetPixels = progress * totalPixels
                                val targetIndex = (targetPixels / itemSize).toInt().coerceIn(0, totalItems - 1)
                                val offset = (targetPixels % itemSize).toInt()
                                
                                if (isGridView) {
                                    gridState.scrollToItem(targetIndex, -offset)
                                } else {
                                    listState.scrollToItem(targetIndex, -offset)
                                }
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
    scrollState: androidx.compose.runtime.State<Pair<Float, Float>>,
    modifier: Modifier = Modifier,
    onScrollTo: (Float) -> Unit
) {
    if (files.size < 10) return 

    val isDraggingState = remember { mutableStateOf(false) }
    val currentLetterState = remember { mutableStateOf<Char?>(null) }
    val currentYState = remember { mutableStateOf(0f) }
    val listSize = files.size

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val trackHeight = constraints.maxHeight.toFloat()
        val minThumbHeight = with(LocalDensity.current) { 48.dp.toPx() }
        
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 2.dp)
                .width(32.dp) 
                .fillMaxHeight()
                .pointerInput(listSize, trackHeight, scrollState) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDraggingState.value = true
                            currentYState.value = offset.y.coerceIn(0f, trackHeight)
                        },
                        onDragEnd = { isDraggingState.value = false },
                        onDragCancel = { isDraggingState.value = false },
                        onVerticalDrag = { change, _ ->
                            val y = change.position.y.coerceIn(0f, trackHeight)
                            currentYState.value = y
                            
                            val (_, fraction) = scrollState.value
                            val thumbHeight = (trackHeight * fraction).coerceAtLeast(minThumbHeight)
                            val travelDistance = trackHeight - thumbHeight
                            
                            val rawProgress = (y - thumbHeight / 2) / travelDistance
                            val progress = rawProgress.coerceIn(0f, 1f)
                            onScrollTo(progress)
                            
                            val index = (progress * (listSize - 1)).toInt()
                            val file = files.getOrNull(index)
                            if (file != null) {
                                currentLetterState.value = file.name.firstOrNull()?.uppercaseChar() ?: '#'
                            }
                        }
                    )
                }
        ) {
              Box(
                  modifier = Modifier
                      .align(Alignment.TopCenter)
                      .width(4.dp)
                      .layout { measurable, constraints ->
                          val (progress, fraction) = scrollState.value
                          val thumbHeightValue = (trackHeight * fraction).coerceAtLeast(minThumbHeight)
                          
                          val placeable = measurable.measure(
                              constraints.copy(
                                  minHeight = thumbHeightValue.toInt(),
                                  maxHeight = thumbHeightValue.toInt()
                              )
                          )
                          
                          val travelDistance = trackHeight - thumbHeightValue
                          val isDragging = isDraggingState.value
                          val currentY = currentYState.value
                          
                          val visualY = if (isDragging) {
                              currentY - thumbHeightValue / 2
                          } else {
                              progress * travelDistance
                          }
                          val effectiveY = visualY.coerceIn(0f, travelDistance)
                          
                          layout(placeable.width, placeable.height) {
                              placeable.place(0, effectiveY.toInt())
                          }
                      }
                      .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
              )
        }

        FastScrollBubble(
            isDraggingState = isDraggingState,
            currentLetterState = currentLetterState,
            currentYState = currentYState,
            trackHeight = trackHeight
        )
    }
}

@Composable
fun FastScrollBubble(
    isDraggingState: androidx.compose.runtime.State<Boolean>,
    currentLetterState: androidx.compose.runtime.State<Char?>,
    currentYState: androidx.compose.runtime.State<Float>,
    trackHeight: Float
) {
    val isDragging = isDraggingState.value
    val currentLetter = currentLetterState.value
    val currentY = currentYState.value

    if (isDragging && currentLetter != null) {
        val bubbleSize = 64.dp
        val bubbleSizePx = with(LocalDensity.current) { bubbleSize.toPx() }
        val bubbleY = (currentY - bubbleSizePx / 2f).coerceIn(0f, trackHeight - bubbleSizePx) 
        
        Box(
            modifier = Modifier
                .fillMaxSize() 
                .wrapContentSize(Alignment.TopEnd) 
                .offset { IntOffset(0, bubbleY.toInt()) }
                .padding(end = 48.dp) 
                .size(bubbleSize)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentLetter.toString(),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

fun shareFiles(context: android.content.Context, files: List<FileModel>, targetAppPackageName: String?) {
    if (files.isEmpty()) return

    val uris = ArrayList<Uri>()
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
            Toast.makeText(context, "Selected app not found or nothing available to share.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
         Toast.makeText(context, "Error preparing share: ${e.message}", Toast.LENGTH_LONG).show()
         e.printStackTrace()
    }
}


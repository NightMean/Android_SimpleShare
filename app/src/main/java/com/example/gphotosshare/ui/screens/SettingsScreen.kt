package com.example.gphotosshare.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gphotosshare.BuildConfig
import com.example.gphotosshare.data.AppModel
import com.example.gphotosshare.data.AppRepository
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.gphotosshare.ui.components.TooltipIconButton
import com.google.accompanist.drawablepainter.rememberDrawablePainter

private enum class SettingsPage { MAIN, APP_SELECTION }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SettingsScreen(
    currentDefaultPath: String,
    currentBrowserPath: String,
    currentTargetAppPackage: String?,
    currentKeepSelection: Boolean,
    currentShowThumbnails: Boolean,
    currentCheckLowStorage: Boolean,
    selectedFileCount: Int,
    onBack: () -> Unit,
    onClearSelection: () -> Unit,
    onSave: (String, String?, Boolean, Boolean, Boolean) -> Unit,
    onReset: () -> Unit
) {
    var path by remember { mutableStateOf(currentDefaultPath) }
    var selectedComponent by remember { mutableStateOf(currentTargetAppPackage) }
    var keepSelection by remember { mutableStateOf(currentKeepSelection) }
    var showThumbnails by remember { mutableStateOf(currentShowThumbnails) }
    var checkLowStorage by remember { mutableStateOf(currentCheckLowStorage) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // Internal Navigation State
    var pageState by remember { mutableStateOf(SettingsPage.MAIN) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val appRepository = remember { AppRepository(context) }
    var apps by remember { mutableStateOf(emptyList<AppModel>()) }
    
    LaunchedEffect(Unit) {
        apps = appRepository.getShareableApps()
    }
    
    // Warning Dialog Trigger
    var showClearSelectionWarning by remember { mutableStateOf(false) }

    // Dirty State Tracking (Initial values vs Current values)
    fun isDirty(): Boolean {
        return path != currentDefaultPath ||
               selectedComponent != currentTargetAppPackage ||
               keepSelection != currentKeepSelection ||
               showThumbnails != currentShowThumbnails ||
               checkLowStorage != currentCheckLowStorage
    }

    var showUnsavedDialog by remember { mutableStateOf(false) }

    // Handle Back Press
    BackHandler {
        if (pageState == SettingsPage.APP_SELECTION) {
             // If searching, clear search first? Or just go back?
             // Standard behavior: Close search if active, or go back.
             // Here we keep it simple: Go back to Main.
            pageState = SettingsPage.MAIN
        } else {
            if (isDirty()) {
                showUnsavedDialog = true
            } else {
                onBack()
            }
        }
    }
    
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved changes. Do you want to save them?") },
            confirmButton = {
                TextButton(onClick = {
                    onSave(path, selectedComponent, keepSelection, showThumbnails, checkLowStorage)
                    showUnsavedDialog = false
                    onBack() // Redirect back after saving
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    // Discard changes? Or just close dialog?
                    // User probably wants to exit.
                    showUnsavedDialog = false
                    onBack() 
                }) { Text("Discard") }
            },
            // Add a Cancel button?
            // "DismissButton" acts as negative action. "Confirm" as positive.
            // Maybe a third button? Standard Dialog has Confirm/Dismiss.
        )
    }
    

    // App Selection State
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (pageState == SettingsPage.APP_SELECTION && isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search apps...") },
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
                         Text(if (pageState == SettingsPage.MAIN) "Settings" else "Select App") 
                    }
                },
                navigationIcon = {
                    TooltipIconButton(
                        onClick = {
                            if (pageState == SettingsPage.APP_SELECTION) {
                                if (isSearchActive) {
                                    isSearchActive = false
                                    searchQuery = ""
                                } else {
                                    pageState = SettingsPage.MAIN
                                }
                            } else {
                                if (isDirty()) {
                                    showUnsavedDialog = true
                                } else {
                                    onBack()
                                }
                            }
                        },
                        tooltip = "Back"
                    ) {
                        Icon(if (isSearchActive) Icons.Default.ArrowBack else Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (pageState == SettingsPage.MAIN) {
                         TextButton(onClick = {
                             onSave(path, selectedComponent, keepSelection, showThumbnails, checkLowStorage)
                         }) {
                             Text("Save")
                         }
                    } else {
                         // APP SELECTION ACTIONS
                         if (!isSearchActive) {
                             IconButton(onClick = { isSearchActive = true }) {
                                 Icon(Icons.Default.Search, contentDescription = "Search")
                             }
                         } else {
                             IconButton(onClick = { 
                                 searchQuery = "" 
                             }) {
                                 Icon(Icons.Default.Close, contentDescription = "Clear")
                             }
                         }
                    }
                }
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = pageState,
            transitionSpec = {
                if (targetState == SettingsPage.APP_SELECTION) {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                } else {
                    slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                }
            },
            label = "SettingsNavigation",
            modifier = Modifier.padding(paddingValues)
        ) { targetPage ->
            if (targetPage == SettingsPage.MAIN) {
                // MAIN PAGE content
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    // Section: Target App
                    item {
                        Text(
                            text = "Target App",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        )
                    }

                    item {
                        val currentApp = apps.find { 
                            val id = "${it.packageName}/${it.activityName}"
                            id == selectedComponent || it.packageName == selectedComponent
                        }
                        
                        ListItem(
                            headlineContent = { Text(currentApp?.name ?: "Select App") },
                            supportingContent = { Text(currentApp?.packageName ?: "No app selected", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingContent = {
                                if (currentApp != null) {
                                    Image(
                                        painter = rememberDrawablePainter(drawable = currentApp.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(40.dp))
                                }
                            },
                            trailingContent = {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Select App")
                            },
                            modifier = Modifier.clickable { pageState = SettingsPage.APP_SELECTION }
                        )
                    }

                    item {
                         Divider(modifier = Modifier.padding(vertical = 16.dp))
                    }

                    // Section: General
                    item {
                        Text(
                            text = "General",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        )
                    }

                    item {
                        // Path Input with Text Button
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            OutlinedTextField(
                                value = path,
                                onValueChange = { path = it },
                                label = { Text("Default Path") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    TextButton(onClick = { path = currentBrowserPath }) {
                                        Text("Use Current")
                                    }
                                },
                                singleLine = true
                            )
                        }
                    }

                    item {
                        ListItem(
                            headlineContent = { Text("Keep Selection") },
                            supportingContent = { Text("Maintain selection across folders") },
                            trailingContent = {
                                Switch(
                                    checked = keepSelection,
                                    onCheckedChange = { newValue ->
                                        if (!newValue && selectedFileCount > 0) showClearSelectionWarning = true
                                        else keepSelection = newValue
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                if (keepSelection && selectedFileCount > 0) showClearSelectionWarning = true
                                else keepSelection = !keepSelection
                            }
                        )
                    }

                    item {
                         ListItem(
                            headlineContent = { Text("Thumbnails") },
                            supportingContent = { Text("Show image previews") },
                            trailingContent = {
                                Switch(
                                    checked = showThumbnails,
                                    onCheckedChange = { showThumbnails = it }
                                )
                            },
                            modifier = Modifier.clickable { showThumbnails = !showThumbnails }
                        )
                    }

                    item {
                        ListItem(
                            headlineContent = { Text("Check Free Space") },
                            supportingContent = { Text("Useful for apps which might copy shared files to internal memory.") },
                            trailingContent = {
                                Switch(
                                    checked = checkLowStorage,
                                    onCheckedChange = { checkLowStorage = it }
                                )
                            },
                            modifier = Modifier.clickable { checkLowStorage = !checkLowStorage }
                        )
                    }
                    
                    item {
                         Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                 TextButton(
                                     onClick = { onReset() }
                                 ) {
                                     Text("Reset to Defaults", color = MaterialTheme.colorScheme.error)
                                 }
                                 
                                 Spacer(modifier = Modifier.height(16.dp))
                                 
                                 val versionName = BuildConfig.VERSION_NAME
                                 val isDebug = BuildConfig.DEBUG
                                 val channel = if (isDebug) "Dev" else "Beta"
                                 Text(
                                     text = "Version $versionName ($channel)",
                                     style = MaterialTheme.typography.labelSmall,
                                     color = MaterialTheme.colorScheme.onSurfaceVariant
                                 )
                             }
                        }
                    }
                }
            } else {
                // APP SELECTION PAGE using Shared Component
                com.example.gphotosshare.ui.components.AppList(
                    apps = apps,
                    searchQuery = searchQuery,
                    selectedPackage = selectedComponent,
                    onAppSelected = { 
                        selectedComponent = it
                        // Auto-close search?
                        if (isSearchActive) {
                            isSearchActive = false
                            searchQuery = ""
                        }
                        pageState = SettingsPage.MAIN
                    }
                )
            }
        }
    }
}

package com.foss.simpleshare.ui.screens

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
import java.io.File
import com.foss.simpleshare.BuildConfig
import com.foss.simpleshare.data.AppModel
import com.foss.simpleshare.data.AppRepository
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import kotlinx.coroutines.launch
import com.foss.simpleshare.ui.components.TooltipIconButton
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
    currentQuickOpen: Boolean,
    currentFilterMode: String, // "PRESET_MEDIA" or "CUSTOM"
    currentCustomExtensions: String,
    selectedFileCount: Int,
    onBack: () -> Unit,
    onSave: (String, String?, Boolean, Boolean, Boolean, Boolean, String, String) -> Unit,
    onReset: () -> Unit
) {
    var path by remember { mutableStateOf(currentDefaultPath) }
    var selectedComponent by remember { mutableStateOf(currentTargetAppPackage) }
    var keepSelection by remember { mutableStateOf(currentKeepSelection) }
    var showThumbnails by remember { mutableStateOf(currentShowThumbnails) }
    var checkLowStorage by remember { mutableStateOf(currentCheckLowStorage) }
    var quickOpen by remember { mutableStateOf(currentQuickOpen) }
    var filterMode by remember { mutableStateOf(currentFilterMode) }
    var customExtensions by remember { mutableStateOf(currentCustomExtensions) }
    var isPathError by remember { mutableStateOf(false) }
    
    // Internal Navigation State
    var pageState by remember { mutableStateOf(SettingsPage.MAIN) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val appRepository = remember { AppRepository(context) }
    var apps by remember { mutableStateOf(emptyList<AppModel>()) }
    var isLoadingApps by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        apps = appRepository.getShareableApps()
        isLoadingApps = false
    }
    
    // Warning Dialog Trigger
    var showClearSelectionWarning by remember { mutableStateOf(false) }

    // Dirty State Tracking (Initial values vs Current values)
    fun isDirty(): Boolean {
        return path != currentDefaultPath ||
               selectedComponent != currentTargetAppPackage ||
               keepSelection != currentKeepSelection ||
               showThumbnails != currentShowThumbnails ||
               checkLowStorage != currentCheckLowStorage ||
               quickOpen != currentQuickOpen ||
               filterMode != currentFilterMode ||
               customExtensions != currentCustomExtensions
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
                    onSave(path, selectedComponent, keepSelection, showThumbnails, checkLowStorage, quickOpen, filterMode, customExtensions)
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
    
    if (showClearSelectionWarning) {
        AlertDialog(
            onDismissRequest = { showClearSelectionWarning = false },
            title = { Text("Disable Keep Selection?") },
            text = { Text("Disabling this will clear your current selection when you save changes.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        keepSelection = false
                        showClearSelectionWarning = false
                    }
                ) {
                    Text("Turn Off")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSelectionWarning = false }) {
                    Text("Cancel")
                }
            }
        )
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
                             // Validation
                             // Validation
                             val file = File(path)
                             if (!file.exists() || !file.isDirectory) {
                                 isPathError = true
                                 android.widget.Toast.makeText(context, "Invalid default folder path. Please enter a valid directory.", android.widget.Toast.LENGTH_LONG).show()
                                 return@TextButton
                             }

                             if (filterMode == "CUSTOM") {
                                 val hasValidChar = customExtensions.any { it.isLetterOrDigit() }
                                 if (!hasValidChar) {
                                     android.widget.Toast.makeText(context, "Please enter at least one file extension (e.g. pdf, zip, 7z)", android.widget.Toast.LENGTH_LONG).show()
                                     return@TextButton
                                 }
                             }
                             onSave(path, selectedComponent, keepSelection, showThumbnails, checkLowStorage, quickOpen, filterMode, customExtensions)
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
        Crossfade(
            targetState = pageState,
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
                    item(key = "header_target_app") {
                        Text(
                            text = "Target App",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        )
                    }

                    item(key = "selector_app") {
                        val currentApp = apps.find { 
                            val id = "${it.packageName}/${it.activityName}"
                            id == selectedComponent || it.packageName == selectedComponent
                        }
                        
                        ListItem(
                            headlineContent = { 
                                val displayText = if (currentApp != null) currentApp.name 
                                                  else if (isLoadingApps && selectedComponent != null) "Loading..."
                                                  else "Select App"
                                Text(displayText)
                            },
                            supportingContent = { 
                                val displayPkg = if (currentApp != null) currentApp.packageName 
                                                 else if (selectedComponent != null && !isLoadingApps) "App not found ($selectedComponent)"
                                                 else if (selectedComponent != null) "Loading..."
                                                 else "No app selected"
                                Text(displayPkg, maxLines = 1, overflow = TextOverflow.Ellipsis) 
                            },
                            leadingContent = {
                                if (currentApp != null) {
                                    Image(
                                        painter = rememberDrawablePainter(drawable = currentApp.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                } else {
                                    // Placeholder
                                    Box(
                                        modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                         if (isLoadingApps && selectedComponent != null) {
                                              // Loading
                                              Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                         } else if (selectedComponent != null) {
                                              // Warning / Not Found
                                              Icon(Icons.Default.Close, contentDescription = "Not Found", tint = MaterialTheme.colorScheme.error)
                                         } else {
                                              // Empty / Check
                                              Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                         }
                                    }
                                }
                            },
                            trailingContent = {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Select App")
                            },
                            modifier = Modifier.clickable { pageState = SettingsPage.APP_SELECTION }
                        )
                    }

                    item(key = "divider_1") {
                         Divider(modifier = Modifier.padding(vertical = 16.dp))
                    }

                    // Section: General
                    item(key = "header_general") {
                        Text(
                            text = "General",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        )
                    }

                    item(key = "input_path") {
                        // Path Input
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            OutlinedTextField(
                                value = path,
                                onValueChange = { 
                                    path = it
                                    isPathError = false
                                },
                                isError = isPathError,
                                label = { 
                                    Text(
                                        if (isPathError) "Invalid Path" else "Default Path",
                                        modifier = Modifier.padding(horizontal = 4.dp).background(MaterialTheme.colorScheme.background)
                                    ) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    TextButton(onClick = { path = currentBrowserPath }) {
                                        Text("Use Current")
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                )
                            )
                        }
                    }

                    item(key = "switch_keep_selection") {
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

                    item(key = "switch_thumbnails") {
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

                    item(key = "switch_check_storage") {
                        ListItem(
                            headlineContent = { Text("Check Free Space") },
                            supportingContent = { Text("Useful for apps which might copy shared files to internal memory") },
                            trailingContent = {
                                Switch(
                                    checked = checkLowStorage,
                                    onCheckedChange = { checkLowStorage = it }
                                )
                            },
                            modifier = Modifier.clickable { checkLowStorage = !checkLowStorage }
                        )
                    }

                    item(key = "switch_quick_open") {
                        ListItem(
                            headlineContent = { Text("Quick Open") },
                            supportingContent = { Text("Long press to open file") },
                            trailingContent = {
                                Switch(
                                    checked = quickOpen,
                                    onCheckedChange = { quickOpen = it }
                                )
                            },
                            modifier = Modifier.clickable { quickOpen = !quickOpen }
                        )
                    }

                    item(key = "divider_2") {
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                    }

                    // Section: Filter
                    item(key = "header_filters") {
                        Text(
                            text = "File Visibility",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        )
                    }
                    
                    item(key = "filter_selector") {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            com.foss.simpleshare.ui.components.FilterModeSelector(
                                selectedMode = filterMode,
                                customExtensions = customExtensions,
                                onModeSelected = { filterMode = it },
                                onCustomExtensionsChanged = { customExtensions = it }
                            )
                        }
                    }

                    item(key = "footer_reset") {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                 TextButton(
                                     onClick = { 
                                         path = currentDefaultPath // Reset path to default
                                         onReset() 
                                     }
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
                // APP SELECTION PAGE
                com.foss.simpleshare.ui.components.AppList(
                    apps = apps,
                    searchQuery = searchQuery,
                    selectedPackage = selectedComponent,
                    onAppSelected = { 
                        selectedComponent = it
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
// End of file

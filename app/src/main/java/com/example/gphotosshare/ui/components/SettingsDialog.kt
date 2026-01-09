package com.example.gphotosshare.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gphotosshare.data.AppModel
import com.example.gphotosshare.data.AppRepository
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun SettingsDialog(
    currentDefaultPath: String,
    currentBrowserPath: String,
    currentTargetAppPackage: String?,
    currentKeepSelection: Boolean,
    currentShowThumbnails: Boolean,
    currentCheckLowStorage: Boolean,
    selectedFileCount: Int,
    onDismiss: () -> Unit,
    onClearSelection: () -> Unit,
    onSave: (String, String?, Boolean, Boolean, Boolean) -> Unit // path, componentName, keepSelection, showThumbnails, checkLowStorage
) {
    var path by remember { mutableStateOf(currentDefaultPath) }
    // Initialize with passed value. componentName is "pkg/cls" or just "pkg" (backward compat)
    val defaultPhotosPackage = "com.google.android.apps.photos" 
    // We try to match by package if component is missing, or exact string match
    var selectedComponent by remember { mutableStateOf(currentTargetAppPackage ?: defaultPhotosPackage) }
    var keepSelection by remember { mutableStateOf(currentKeepSelection) }
    var showThumbnails by remember { mutableStateOf(currentShowThumbnails) }
    var checkLowStorage by remember { mutableStateOf(currentCheckLowStorage) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val appRepository = remember { AppRepository(context) }
    var apps by remember { mutableStateOf(emptyList<AppModel>()) }
    
    LaunchedEffect(Unit) {
        apps = appRepository.getShareableApps()
    }
    
    // State for warning dialog
    var showClearSelectionWarning by remember { mutableStateOf(false) }

    if (showClearSelectionWarning) {
        AlertDialog(
            onDismissRequest = { showClearSelectionWarning = false },
            title = { Text("Clear Selection?") },
            text = { Text("Some files are already selected. Do you want to continue? This will clear your current selection.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearSelection()
                        keepSelection = false
                        showClearSelectionWarning = false
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearSelectionWarning = false }
                ) {
                    Text("No")
                }
            }
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Default Path")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Path") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { path = currentBrowserPath },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use Current Directory")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Toggle: Keep Selection
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { 
                        val newValue = !keepSelection
                        if (!newValue && selectedFileCount > 0) {
                            showClearSelectionWarning = true
                        } else {
                            keepSelection = newValue
                        }
                    }
                ) {
                    Text("Keep selection across folders", modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(
                        checked = keepSelection,
                        onCheckedChange = { newValue ->
                            if (!newValue && selectedFileCount > 0) {
                                showClearSelectionWarning = true
                            } else {
                                keepSelection = newValue
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Toggle: Thumbnails
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { showThumbnails = !showThumbnails }
                ) {
                    Text("Generate thumbnails", modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(
                        checked = showThumbnails,
                        onCheckedChange = { showThumbnails = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                // Toggle: Check Low Storage
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { checkLowStorage = !checkLowStorage }
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                         Text("Check Free Space")
                         Text(
                            "Warn before sharing if internal storage is low",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = checkLowStorage,
                        onCheckedChange = { checkLowStorage = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Default Share App")
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier
                        .height(300.dp)
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                ) {
                   items(apps.size) { index ->
                       val app = apps[index]
                       // Logic to determine unique ID. If app has same package but different activity, we need robust check.
                       // User wants "pkg/cls" to be saved.
                       val componentString = "${app.packageName}/${app.activityName}"
                       val isSelected = componentString == selectedComponent || selectedComponent == app.packageName // Fallback match
                       
                       Row(
                           modifier = Modifier
                               .fillMaxWidth()
                               .clickable { selectedComponent = componentString }
                               .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                               .padding(8.dp),
                           verticalAlignment = Alignment.CenterVertically
                       ) {
                           Image(
                               painter = rememberDrawablePainter(drawable = app.icon),
                               contentDescription = null,
                               modifier = Modifier.size(40.dp)
                           )
                           Spacer(modifier = Modifier.width(12.dp))
                           Column {
                               Text(
                                   text = app.name,
                                   style = MaterialTheme.typography.bodyMedium,
                                   maxLines = 1,
                                   overflow = TextOverflow.Ellipsis
                               )
                               Text(
                                   text = app.packageName, 
                                   style = MaterialTheme.typography.labelSmall,
                                   color = MaterialTheme.colorScheme.onSurfaceVariant,
                                   maxLines = 1,
                                   overflow = TextOverflow.Ellipsis
                               )
                           }
                           
                           if (isSelected) {
                               Spacer(modifier = Modifier.weight(1f))
                               Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(24.dp))
                           }
                       }
                       Divider()
                   }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                onSave(path, selectedComponent, keepSelection, showThumbnails, checkLowStorage) 
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

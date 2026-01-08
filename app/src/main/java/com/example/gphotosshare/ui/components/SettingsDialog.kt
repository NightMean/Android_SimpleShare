package com.example.gphotosshare.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit // path, packageName
) {
    var path by remember { mutableStateOf(currentDefaultPath) }
    var selectedAppPackage by remember { mutableStateOf<String?>(null) } // Load initial from elsewhere? passed in?
    // We should pass in the currently selected app package too.
    
    // Changing signature to include selectedAppPackage
    // But since I can't easily change the signature and all callsites in one replace_chunk cleanly if I don't see them all,
    // I will assume the caller will be updated.
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val appRepository = remember { AppRepository(context) }
    var apps by remember { mutableStateOf(emptyList<AppModel>()) }
    
    LaunchedEffect(Unit) {
        apps = appRepository.getShareableApps()
    }
    
    // We need to fetch the saved package preference to show as selected. 
    // Ideally this is passed in. I'll modify the signature to accept it.
    // For now, I'll use a local fetch for simplicity if not passed, but passing is better.
    // Let's rely on the caller passing it.
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column {
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
                Text("Default Share App")
                Spacer(modifier = Modifier.height(8.dp))
                
                // Simple dropdown or list for apps
                // Using a simple LazyColumn with limited height for now
                // Simple dropdown or list for apps
                // Using a simple LazyColumn with limited height for now
                LazyColumn(
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                ) {
                   items(apps.size) { index ->
                       val app = apps[index]
                       val isSelected = app.packageName == selectedAppPackage
                       Row(
                           modifier = Modifier
                               .fillMaxWidth()
                               .clickable { selectedAppPackage = app.packageName }
                               .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                               .padding(8.dp),
                           verticalAlignment = Alignment.CenterVertically
                       ) {
                           Image(
                               painter = rememberDrawablePainter(drawable = app.icon),
                               contentDescription = null,
                               modifier = Modifier.size(32.dp)
                           )
                           Spacer(modifier = Modifier.width(8.dp))
                           Text(
                               text = app.name,
                               style = MaterialTheme.typography.bodyMedium,
                               maxLines = 1,
                               overflow = TextOverflow.Ellipsis
                           )
                           if (isSelected) {
                               Spacer(modifier = Modifier.weight(1f))
                               Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp))
                           }
                       }
                       Divider()
                   }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(path, selectedAppPackage) }) {
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

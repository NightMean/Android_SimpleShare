package com.example.gphotosshare.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.gphotosshare.data.AppModel
import com.example.gphotosshare.data.AppRepository
import com.example.gphotosshare.ui.components.AppList

import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    currentScreen: com.example.gphotosshare.ui.Screen, 
    permissionGranted: Boolean,
    selectedTargetApp: String?,
    onRequestPermission: () -> Unit,
    onAppSelected: (String) -> Unit,
    onFinish: () -> Unit,
    onNavigateToAppSelection: () -> Unit,
    onBackFromAppSelection: () -> Unit
) {
    if (currentScreen == com.example.gphotosshare.ui.Screen.SETUP_APP_SELECTION) {
        // App Selection View
        val context = LocalContext.current
        val appRepository = remember { AppRepository(context) }
        var apps by remember { mutableStateOf(emptyList<AppModel>()) }
        
        var searchQuery by remember { mutableStateOf("") }
        var isSearchActive by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            apps = appRepository.getShareableApps()
        }
        
        LaunchedEffect(isSearchActive) {
            if (isSearchActive) {
                focusRequester.requestFocus()
            }
        }
        
        Scaffold(
            topBar = {
                 androidx.compose.material3.TopAppBar(
                     title = { 
                         if (isSearchActive) {
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
                             Text("Select Target App")
                         }
                     },
                     navigationIcon = {
                         androidx.compose.material3.IconButton(onClick = {
                             if (isSearchActive) {
                                 isSearchActive = false
                                 searchQuery = ""
                             } else {
                                 onBackFromAppSelection()
                             }
                         }) {
                             Icon(if (isSearchActive) Icons.Default.ArrowBack else Icons.Default.ArrowBack, contentDescription = "Back")
                         }
                     },
                     actions = {
                         if (!isSearchActive) {
                             androidx.compose.material3.IconButton(onClick = { isSearchActive = true }) {
                                 Icon(Icons.Default.Search, contentDescription = "Search")
                             }
                         } else {
                             androidx.compose.material3.IconButton(onClick = { searchQuery = "" }) {
                                 Icon(Icons.Default.Close, contentDescription = "Clear")
                             }
                         }
                     }
                 )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                AppList(
                    apps = apps,
                    searchQuery = searchQuery,
                    selectedPackage = selectedTargetApp,
                    onAppSelected = { 
                        onAppSelected(it)
                        onBackFromAppSelection()
                    }
                )
            }
        }
    } else {
        // Main Setup View
        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                // Step 1: Permission
                SetupItem(
                    title = "1. Grant Permissions",
                    description = "We need access to your files to share them.",
                    action = {
                        if (permissionGranted) {
                             Button(
                                 onClick = {}, 
                                 enabled = false,
                                 colors = ButtonDefaults.buttonColors(
                                    disabledContainerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                                    disabledContentColor = androidx.compose.ui.graphics.Color.White
                                 )
                             ) {
                                 Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                 Spacer(modifier = Modifier.width(8.dp))
                                 Text("Granted")
                             }
                        } else {
                            Button(onClick = onRequestPermission) {
                                Text("Grant Access")
                            }
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Keep track of current app Model to display icon
                val context = LocalContext.current
                val appRepository = remember { AppRepository(context) }
                var apps by remember { mutableStateOf(emptyList<AppModel>()) }
                LaunchedEffect(Unit) { apps = appRepository.getShareableApps() }
                
                val currentAppModel = remember(selectedTargetApp, apps) {
                    if (selectedTargetApp == null) null
                    else apps.find { "${it.packageName}/${it.activityName}" == selectedTargetApp || it.packageName == selectedTargetApp }
                }

                // Step 2: Target App
                SetupItem(
                    title = "2. Select Target App",
                    description = "Choose the app you want to share files to.",
                    action = {
                         if (selectedTargetApp != null) {
                             Button(onClick = onNavigateToAppSelection) {
                                 Text("Change App")
                             }
                         } else {
                             Button(onClick = onNavigateToAppSelection) {
                                 Text("Select App")
                             }
                         }
                    }
                )
                
                if (currentAppModel != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Current Target App: ", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(8.dp))
                            Image(
                                painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(drawable = currentAppModel.icon),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(currentAppModel.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = currentAppModel.packageName, 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Button(
                    onClick = onFinish,
                    enabled = permissionGranted && selectedTargetApp != null,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Get Started")
                }
            }
        }
    }
}

@Composable
fun SetupItem(
    title: String,
    description: String,
    action: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description, 
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        action()
    }
}

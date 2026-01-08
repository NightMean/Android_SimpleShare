package com.example.gphotosshare

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.gphotosshare.data.FileRepository
import com.example.gphotosshare.ui.components.SettingsDialog
import com.example.gphotosshare.ui.screens.FileBrowserScreen
import com.example.gphotosshare.ui.theme.GPhotosShareTheme

class MainActivity : ComponentActivity() {

    private val PREFS_NAME = "gphotos_share_prefs"
    private val KEY_DEFAULT_PATH = "default_path"
    private val KEY_TARGET_APP = "target_app_package"
    private val KEY_KEEP_SELECTION = "keep_selection"
    private val KEY_SHOW_THUMBNAILS = "show_thumbnails"
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setContent {
            GPhotosShareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }
    }

    @Composable
    fun MainContent() {
        var hasPermission by remember { mutableStateOf(checkStoragePermission()) }
        var showSettings by remember { mutableStateOf(false) }
        
        // Define default path from prefs, but also keep track of current browsing path
        val savedDefaultPath = prefs.getString(KEY_DEFAULT_PATH, FileRepository().getDefaultPath()) ?: FileRepository().getDefaultPath()
        val savedTargetApp = prefs.getString(KEY_TARGET_APP, null)
        val savedKeepSelection = prefs.getBoolean(KEY_KEEP_SELECTION, true) // Default true
        val savedShowThumbnails = prefs.getBoolean(KEY_SHOW_THUMBNAILS, true) // Default true
        
        // We initialize currentPath with savedDefaultPath, but it's now state managed here
        var currentPath by remember { mutableStateOf(savedDefaultPath) }
        var targetAppPackage by remember { mutableStateOf(savedTargetApp) }
        var keepSelection by remember { mutableStateOf(savedKeepSelection) }
        var showThumbnails by remember { mutableStateOf(savedShowThumbnails) }
        
        // Hoisted selection state
        val selectedFiles = remember { androidx.compose.runtime.mutableStateListOf<com.example.gphotosshare.data.FileModel>() }
        
        // Handle Coil Cache clearing when disabled
        val context = androidx.compose.ui.platform.LocalContext.current
        LaunchedEffect(showThumbnails) {
            if (!showThumbnails) {
                // Clear memory cache to free up resources as requested
                coil.Coil.imageLoader(context).memoryCache?.clear()
            }
        }

        // This is a simple state trigger to refresh UI after returning from Settings
        val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
        androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    hasPermission = checkStoragePermission()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        if (hasPermission) {
            val repository = remember { FileRepository() }
            
            FileBrowserScreen(
                repository = repository,
                initialPath = savedDefaultPath,
                currentPath = currentPath,
                onPathChange = { newPath -> currentPath = newPath },
                selectedFiles = selectedFiles,
                targetAppPackageName = targetAppPackage,
                keepSelection = keepSelection,
                showThumbnails = showThumbnails,
                onSettingsClick = { showSettings = true }
            )
            
            if (showSettings) {
                SettingsDialog(
                    currentDefaultPath = savedDefaultPath,
                    currentBrowserPath = currentPath,
                    currentTargetAppPackage = targetAppPackage,
                    currentKeepSelection = keepSelection,
                    currentShowThumbnails = showThumbnails,
                    selectedFileCount = selectedFiles.size,
                    onDismiss = { showSettings = false },
                    onClearSelection = { selectedFiles.clear() },
                    onSave = { newPath, newAppPackage, newKeepSelection, newShowThumbnails ->
                        val editor = prefs.edit()
                        editor.putString(KEY_DEFAULT_PATH, newPath)
                        if (newAppPackage != null) {
                            editor.putString(KEY_TARGET_APP, newAppPackage)
                            targetAppPackage = newAppPackage
                        } else {
                            editor.remove(KEY_TARGET_APP)
                            targetAppPackage = null
                        }
                        editor.putBoolean(KEY_KEEP_SELECTION, newKeepSelection)
                        keepSelection = newKeepSelection
                        
                        editor.putBoolean(KEY_SHOW_THUMBNAILS, newShowThumbnails)
                        showThumbnails = newShowThumbnails
                        
                        editor.apply()
                        
                        showSettings = false
                    }
                )
            }

        } else {
            PermissionScreen(
                onRequestPermission = {
                    requestStoragePermission()
                }
            )
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            // For < Android 11, basic read/write usually enough, but we want full manager access logic mapping
            // Actually for < 11 we rely on standard permissions
            // Since User says "Target SDK 34, Min 28", checkStoragePermission() should cover both cases.
             checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse(String.format("package:%s", packageName))
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivity(intent)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1001
            )
        }
    }
}

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Access to all files is required to browse and share media.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequestPermission) {
                Text("Grant Permissions")
            }
        }
    }
}

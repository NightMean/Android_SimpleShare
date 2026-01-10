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
// import com.example.gphotosshare.ui.components.SettingsDialog (Deleted)
import com.example.gphotosshare.ui.screens.FileBrowserScreen
import com.example.gphotosshare.ui.theme.GPhotosShareTheme

class MainActivity : ComponentActivity() {

    private val PREFS_NAME = "gphotos_share_prefs"
    private val KEY_DEFAULT_PATH = "default_path"
    private val KEY_TARGET_APP = "target_app_package"
    private val KEY_KEEP_SELECTION = "keep_selection"

    private val KEY_SHOW_THUMBNAILS = "show_thumbnails"
    private val KEY_CHECK_LOW_STORAGE = "check_low_storage"

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
        // State for Screen Navigation
        val savedTargetApp = prefs.getString(KEY_TARGET_APP, null)
        var targetAppPackage by remember { mutableStateOf(savedTargetApp) }
        
        var currentScreen by remember {
            mutableStateOf(
                if (!hasPermission || targetAppPackage == null) com.example.gphotosshare.ui.Screen.SETUP 
                else com.example.gphotosshare.ui.Screen.BROWSER
            )
        }
        
        // Define default path from prefs, but also keep track of current browsing path
        val savedDefaultPath = prefs.getString(KEY_DEFAULT_PATH, FileRepository().getDefaultPath()) ?: FileRepository().getDefaultPath()
        val savedKeepSelection = prefs.getBoolean(KEY_KEEP_SELECTION, true) // Default true
        val savedShowThumbnails = prefs.getBoolean(KEY_SHOW_THUMBNAILS, true) // Default true
        val savedCheckLowStorage = prefs.getBoolean(KEY_CHECK_LOW_STORAGE, false) // Default false, as per request

        // We initialize currentPath with savedDefaultPath
        var currentPath by remember { mutableStateOf(savedDefaultPath) }
        var keepSelection by remember { mutableStateOf(savedKeepSelection) }
        var showThumbnails by remember { mutableStateOf(savedShowThumbnails) }
        var checkLowStorage by remember { mutableStateOf(savedCheckLowStorage) }
        
        // Hoisted selection state
        val selectedFiles = remember { androidx.compose.runtime.mutableStateListOf<com.example.gphotosshare.data.FileModel>() }
        
        // Handle Coil Cache clearing when disabled
        val context = androidx.compose.ui.platform.LocalContext.current
        LaunchedEffect(showThumbnails) {
            if (!showThumbnails) {
                com.bumptech.glide.Glide.get(context).clearMemory()
            }
        }

        // This is a simple state trigger to refresh UI after returning from Settings
        val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
        androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    val newPermission = checkStoragePermission()
                    if (newPermission != hasPermission) {
                        hasPermission = newPermission
                        // If we gain permission and we were in SETUP, we should stay in SETUP until App is selected
                        // If we lose permission, we must go to SETUP
                        if (!newPermission) {
                            currentScreen = com.example.gphotosshare.ui.Screen.SETUP
                        }
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        if (false) { 
             // Legacy PermissionScreen block removed, handled by SetupScreen
        }

        val repository = remember { FileRepository() }
        
        // NAVIGATION HOST
        when (currentScreen) {
            com.example.gphotosshare.ui.Screen.BROWSER -> {
                if (hasPermission) {
                    FileBrowserScreen(
                        repository = repository,
                        currentPath = currentPath,
                        onPathChange = { newPath -> currentPath = newPath },
                        selectedFiles = selectedFiles,
                        targetAppPackageName = targetAppPackage,
                        keepSelection = keepSelection,
                        showThumbnails = showThumbnails,
                        checkLowStorage = checkLowStorage,
                        onSettingsClick = { currentScreen = com.example.gphotosshare.ui.Screen.SETTINGS }
                    )
                } else {
                    // Fallback if permission lost
                    currentScreen = com.example.gphotosshare.ui.Screen.SETUP
                }
            }
            com.example.gphotosshare.ui.Screen.SETTINGS -> {
                com.example.gphotosshare.ui.screens.SettingsScreen(
                    currentDefaultPath = savedDefaultPath,
                    currentBrowserPath = currentPath,
                    currentTargetAppPackage = targetAppPackage, // Can be null now
                    currentKeepSelection = keepSelection,
                    currentShowThumbnails = showThumbnails,
                    currentCheckLowStorage = checkLowStorage,
                    selectedFileCount = selectedFiles.size,
                    onBack = { currentScreen = com.example.gphotosshare.ui.Screen.BROWSER },
                    onClearSelection = { selectedFiles.clear() },
                    onSave = { path, targetApp, keepSel, showIcons, checkSpace ->
                        val editor = prefs.edit()
                        editor.putString(KEY_DEFAULT_PATH, path)
                        if (targetApp != null) {
                            editor.putString(KEY_TARGET_APP, targetApp)
                            targetAppPackage = targetApp
                        } else {
                            editor.remove(KEY_TARGET_APP)
                            targetAppPackage = null
                        }
                        editor.putBoolean(KEY_KEEP_SELECTION, keepSel)
                        keepSelection = keepSel
                        
                        editor.putBoolean(KEY_SHOW_THUMBNAILS, showIcons)
                        showThumbnails = showIcons
                        
                        editor.putBoolean(KEY_CHECK_LOW_STORAGE, checkSpace)
                        checkLowStorage = checkSpace

                        editor.apply()
                        
                        android.widget.Toast.makeText(context, "Settings saved", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onReset = {
                        val editor = prefs.edit()
                        editor.clear()
                        editor.apply()
                        
                        targetAppPackage = null
                        keepSelection = false
                        showThumbnails = true
                        checkLowStorage = false
                        
                        // Force back to SETUP because targetApp is null
                        currentScreen = com.example.gphotosshare.ui.Screen.SETUP
                        
                        android.widget.Toast.makeText(context, "Reset to defaults.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
            com.example.gphotosshare.ui.Screen.SETUP,
            com.example.gphotosshare.ui.Screen.SETUP_APP_SELECTION -> {
                com.example.gphotosshare.ui.screens.SetupScreen(
                    currentScreen = currentScreen,
                    permissionGranted = hasPermission,
                    selectedTargetApp = targetAppPackage,
                    onRequestPermission = { requestStoragePermission() },
                    onAppSelected = { app -> 
                         targetAppPackage = app
                         val editor = prefs.edit()
                         editor.putString(KEY_TARGET_APP, app)
                         editor.apply()
                    },
                    onFinish = {
                        currentScreen = com.example.gphotosshare.ui.Screen.BROWSER
                    },
                    onNavigateToAppSelection = {
                        currentScreen = com.example.gphotosshare.ui.Screen.SETUP_APP_SELECTION
                    },
                    onBackFromAppSelection = {
                        currentScreen = com.example.gphotosshare.ui.Screen.SETUP
                    }
                )
            }
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

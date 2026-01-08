package com.example.gphotosshare.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class AppRepository(private val context: Context) {

    fun getShareableApps(): List<AppModel> {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*" // Query for image handlers, most video handlers overlap
        }

        val packageManager = context.packageManager
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }

        return resolveInfos.mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo
            val packageName = activityInfo.packageName
            
            // Filter out own app if it shows up
            if (packageName == context.packageName) return@mapNotNull null

            try {
                val appName = resolveInfo.loadLabel(packageManager).toString()
                val icon = resolveInfo.loadIcon(packageManager)
                AppModel(appName, packageName, icon)
            } catch (e: Exception) {
                null
            }
        }.distinctBy { it.packageName }.sortedBy { it.name }
    }
}

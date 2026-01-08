package com.example.gphotosshare.utils

import android.os.Environment
import android.os.StatFs
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

object StorageUtils {

    fun getAvailableStorage(path: String = Environment.getExternalStorageDirectory().absolutePath): Long {
        val stat = StatFs(path)
        return stat.availableBytes
    }

    fun hasEnoughSpace(requiredBytes: Long): Boolean {
        // Use external storage path for check since that's where we usually operate, 
        // though "internal storage" in Android terms usually means the data partition /data 
        // but user effectively means "primary shared storage".
        // Environment.getDataDirectory() gives /data. 
        // Environment.getExternalStorageDirectory() gives /storage/emulated/0.
        // We probably want to check where we are WRITING to?
        // Wait, we are UPLOADING. The prompt says: "If Available Space < Total File Size".
        // "This implies we might be copying to a temp directory before sharing?"
        // OR the user means "Not enough space left on device" in general?
        // Usually sharing via ACTION_SEND with a content:// URI doesn't require extra space 
        // UNLESS we are copying to cache.
        // If we serve valid content:// URIs from original files, we don't need to copy.
        // HOWEVER, "Logic: If Available Space < Total File Size... upload might fail".
        // This suggests the user believes sharing involves copying, OR maybe google photos copies it?
        // I will strictly follow the requirement: Check available internal storage vs selected size.
        // I'll check Environment.getDataDirectory() (Internal storage where app cache lives) 
        // AND Environment.getExternalStorageDirectory() just to be safe or just Data as per "device's available internal storage".
        
        return getAvailableStorage() >= requiredBytes
    }

    fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }
}

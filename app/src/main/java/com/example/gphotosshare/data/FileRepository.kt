package com.example.gphotosshare.data

import java.io.File
import java.util.Locale

class FileRepository {

    private val supportedExtensions = setOf(
        "jpg", "png", "gif", "mp4", "mkv", "webm", "avi", "heic"
    )

    fun listFiles(path: String): List<FileModel> {
        val root = File(path)
        if (!root.exists() || !root.isDirectory) return emptyList()

        return root.listFiles()?.mapNotNull { file ->
            val isDirectory = file.isDirectory
            val extension = file.extension.lowercase(Locale.getDefault())
            
            // Allow directories or supported media files
            if (isDirectory || supportedExtensions.contains(extension)) {
                FileModel(
                    file = file,
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = isDirectory,
                    size = if (isDirectory) 0 else file.length(),
                    extension = extension
                )
            } else {
                null
            }
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.getDefault()) })) 
        ?: emptyList()
    }

    fun getDefaultPath(): String {
        return "/storage/emulated/0"
    }
}

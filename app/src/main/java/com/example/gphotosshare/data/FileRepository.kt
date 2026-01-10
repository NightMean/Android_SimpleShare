package com.example.gphotosshare.data

import java.io.File
import java.util.Locale

class FileRepository {

    fun listFiles(path: String, allowedExtensions: Set<String>): List<FileModel> {
        val root = File(path)
        if (!root.exists() || !root.isDirectory) return emptyList()

        return root.listFiles()?.mapNotNull { file ->
            val isDirectory = file.isDirectory
            val extension = file.extension.lowercase(Locale.getDefault())
            
            // Allow directories OR if the file extension is in the allowed set
            // If the set is empty, we might default to nothing or everything? 
            // Design decision: If set is empty, show nothing (except folders). 
            // Wrapper will ensure set is populated.
            if (isDirectory || allowedExtensions.contains(extension)) {
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

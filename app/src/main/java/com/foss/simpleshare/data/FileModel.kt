package com.foss.simpleshare.data

import java.io.File

data class FileModel(
    val file: File,
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val extension: String,
    val itemCount: Int = 0,
    var isSelected: Boolean = false
)

package com.foss.simpleshare.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Caches folder sizes to avoid expensive recursive calculations on every load.
 * Size is stored along with lastModified timestamp for invalidation checks.
 */
@Entity(tableName = "directory_cache")
data class DirectoryCache(
    @PrimaryKey val path: String,
    val size: Long,
    val lastModified: Long
)

package com.foss.simpleshare.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for DirectoryCache - handles cache lookups, insertions, and invalidation.
 */
@Dao
interface DirectoryCacheDao {
    
    /**
     * Get cached size for a specific path.
     */
    @Query("SELECT * FROM directory_cache WHERE path = :path LIMIT 1")
    suspend fun getByPath(path: String): DirectoryCache?
    
    /**
     * Insert or update a cache entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: DirectoryCache)
    
    /**
     * Invalidate cache for a specific path (when folder is modified).
     */
    @Query("DELETE FROM directory_cache WHERE path = :path")
    suspend fun deleteByPath(path: String)
    
    /**
     * Invalidate all cache entries that start with a prefix (for subtree invalidation).
     */
    @Query("DELETE FROM directory_cache WHERE path LIKE :pathPrefix || '%'")
    suspend fun deleteByPathPrefix(pathPrefix: String)
}

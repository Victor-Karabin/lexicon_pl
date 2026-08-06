package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ImageUrlCacheDao {
    @Query("SELECT imageUrl FROM image_url_cache WHERE query = :query")
    suspend fun get(query: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ImageUrlCacheEntity)
}

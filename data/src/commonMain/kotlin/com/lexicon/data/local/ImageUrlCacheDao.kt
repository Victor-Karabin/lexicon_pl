package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ImageUrlCacheDao {
    @Query("SELECT * FROM image_url_cache WHERE query = :query")
    suspend fun find(query: String): ImageUrlCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ImageUrlCacheEntity)
}

package com.lexicon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Caches a resolved image URL per search query so the image-provider APIs aren't re-hit every session. */
@Entity(tableName = "image_url_cache")
data class ImageUrlCacheEntity(
    @PrimaryKey val query: String,
    val imageUrl: String,
)

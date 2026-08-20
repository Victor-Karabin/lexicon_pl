package com.lexicon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image_url_cache")
data class ImageUrlCacheEntity(
    @PrimaryKey val query: String,
    val imageUrl: String,
)

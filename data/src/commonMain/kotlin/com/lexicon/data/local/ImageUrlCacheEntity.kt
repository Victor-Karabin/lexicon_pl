package com.lexicon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

const val NO_IMAGE = ""

@Entity(tableName = "image_url_cache")
data class ImageUrlCacheEntity(
    @PrimaryKey val query: String,
    val imageUrl: String,
    val checkedAtEpochMillis: Long = 0,
)

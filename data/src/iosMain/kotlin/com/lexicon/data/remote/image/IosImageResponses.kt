package com.lexicon.data.remote.image

import kotlinx.serialization.Serializable

// The Android responses live beside the Retrofit interfaces that declare them, in a
// source set iOS cannot see. Same shapes, so the same JSON parses on both.

@Serializable
data class OpenverseSearchResponse(val results: List<OpenverseImage> = emptyList())

@Serializable
data class OpenverseImage(val url: String)

@Serializable
data class PexelsSearchResponse(val photos: List<PexelsPhoto> = emptyList())

@Serializable
data class PexelsPhoto(val src: PexelsPhotoSource)

@Serializable
data class PexelsPhotoSource(val medium: String)

@Serializable
data class PixabaySearchResponse(val hits: List<PixabayHit> = emptyList())

@Serializable
data class PixabayHit(val webformatURL: String)

package com.lexicon.data.remote.image

import kotlinx.serialization.Serializable

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

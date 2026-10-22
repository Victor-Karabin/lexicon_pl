package com.lexicon.data.local

expect class AssetReader {
    fun readText(fileName: String): String

    fun exists(fileName: String): Boolean
}

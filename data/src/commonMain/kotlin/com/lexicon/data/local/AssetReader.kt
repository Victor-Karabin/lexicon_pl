package com.lexicon.data.local

/**
 * Reads a bundled JSON catalogue. Android serves these out of assets/, iOS out of
 * the app bundle, so the lookup itself is the only per-platform part — parsing and
 * fingerprinting stay common (see the three *AssetLoader classes).
 */
expect class AssetReader {
    fun readText(fileName: String): String
}

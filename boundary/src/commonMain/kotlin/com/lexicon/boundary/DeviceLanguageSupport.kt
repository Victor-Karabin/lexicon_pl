package com.lexicon.boundary

interface DeviceLanguageSupport {
    suspend fun hasPolishKeyboard(): Boolean

    suspend fun hasPolishVoice(): Boolean
}

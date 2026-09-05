package com.lexicon.interactors.settings

fun interface UpdateVoiceUseCase {
    suspend operator fun invoke(voiceId: String?)
}

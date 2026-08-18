package com.lexicon.interactors.settings

interface UpdateVoiceUseCase {
    suspend operator fun invoke(voiceId: String?)
}

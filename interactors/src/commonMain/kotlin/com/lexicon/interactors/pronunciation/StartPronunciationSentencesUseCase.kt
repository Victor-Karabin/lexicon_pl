package com.lexicon.interactors.pronunciation

sealed interface PronunciationSentencesResult {
    data class Ready(val session: PronunciationSessionResponse) : PronunciationSentencesResult

    data object NoFavourites : PronunciationSentencesResult

    data object Offline : PronunciationSentencesResult

    data class Refused(val reason: String) : PronunciationSentencesResult
}

/** Sentences to read aloud, written for the words you are learning. */
interface StartPronunciationSentencesUseCase {
    suspend operator fun invoke(): PronunciationSentencesResult
}

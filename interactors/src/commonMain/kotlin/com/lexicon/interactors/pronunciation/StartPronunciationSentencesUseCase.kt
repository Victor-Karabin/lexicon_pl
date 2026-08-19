package com.lexicon.interactors.pronunciation

sealed interface PronunciationSentencesResult {
    data class Ready(val session: PronunciationSessionResponse) : PronunciationSentencesResult

    data object EmptyStudySet : PronunciationSentencesResult

    data object Offline : PronunciationSentencesResult

    data class Refused(val reason: String) : PronunciationSentencesResult
}

interface StartPronunciationSentencesUseCase {
    suspend operator fun invoke(): PronunciationSentencesResult
}

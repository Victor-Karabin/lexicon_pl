package com.lexicon.application.pronunciation

import com.lexicon.application.settings.StepCountResolver
import com.lexicon.application.training.failure
import com.lexicon.application.training.maxLevel
import com.lexicon.application.training.open
import com.lexicon.application.training.sentencesFor
import com.lexicon.application.training.wordsToStudy
import com.lexicon.boundary.SentenceGenerator
import com.lexicon.boundary.SentenceResultBoundary
import com.lexicon.boundary.SessionStore
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.pronunciation.PronunciationSentencesResult
import com.lexicon.interactors.pronunciation.PronunciationSessionResponse
import com.lexicon.interactors.pronunciation.PronunciationStepResponse
import com.lexicon.interactors.pronunciation.StartPronunciationSentencesUseCase
import com.lexicon.interactors.pronunciation.StartPronunciationSessionRequest
import com.lexicon.model.training.TrainingType

class StartPronunciationSentencesUseCaseImpl(
    private val vocabulary: VocabularyRepository,
    private val generator: SentenceGenerator,
    private val stepCountResolver: StepCountResolver,
    private val sessions: SessionStore,
) : StartPronunciationSentencesUseCase {
    override suspend fun invoke(request: StartPronunciationSessionRequest): PronunciationSentencesResult {
        val studySet = vocabulary.wordsToStudy(request.vocabularyIds)
        if (studySet.isEmpty()) return PronunciationSentencesResult.EmptyStudySet

        val level = studySet.maxLevel()
        val wanted = stepCountResolver.resolve(request.stepCount).coerceAtMost(studySet.size)
        val targets = studySet.shuffled().take(wanted)

        val generated = generator.sentencesFor(targets, level)

        val sentences = generated.mapNotNull { (word, result) ->
            (result as? SentenceResultBoundary.Generated)?.sentence?.trim()?.takeIf { it.isNotEmpty() }?.let { word to it }
        }
        val steps = sentences.mapIndexed { index, (word, sentence) ->
            PronunciationStepResponse(
                stepIndex = index,
                vocabularyItemId = word.id.value,
                expectedText = sentence,
                clueText = sentence,
                transcription = "",
            )
        }
        if (steps.isEmpty()) {
            return when (val failure = generated.map { it.second }.failure()) {
                SentenceResultBoundary.Offline -> PronunciationSentencesResult.Offline
                is SentenceResultBoundary.Refused -> PronunciationSentencesResult.Refused(failure.reason)
                else -> PronunciationSentencesResult.Refused("no sentence came back")
            }
        }

        val sessionId = sessions.open(TrainingType.PRONUNCIATION_SENTENCES, sentences.map { (word, sentence) -> word.id to sentence })

        return PronunciationSentencesResult.Ready(
            PronunciationSessionResponse(sessionId = sessionId.value, steps = steps),
        )
    }
}

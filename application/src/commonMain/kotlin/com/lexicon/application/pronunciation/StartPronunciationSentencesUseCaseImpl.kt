package com.lexicon.application.pronunciation

import com.lexicon.application.passage.maxLevel
import com.lexicon.application.settings.StepCountResolver
import com.lexicon.boundary.SentenceGenerator
import com.lexicon.boundary.SentenceRequestBoundary
import com.lexicon.boundary.SentenceResultBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.pronunciation.PronunciationSentencesResult
import com.lexicon.interactors.pronunciation.PronunciationSessionResponse
import com.lexicon.interactors.pronunciation.PronunciationStepResponse
import com.lexicon.interactors.pronunciation.StartPronunciationSentencesUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class StartPronunciationSentencesUseCaseImpl(
    private val vocabulary: VocabularyRepository,
    private val generator: SentenceGenerator,
    private val stepCountResolver: StepCountResolver,
) : StartPronunciationSentencesUseCase {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun invoke(): PronunciationSentencesResult {
        val studySet = vocabulary.getItemsByIds(vocabulary.studySetWordIds())
        if (studySet.isEmpty()) return PronunciationSentencesResult.EmptyStudySet

        val level = studySet.maxLevel()
        val wanted = stepCountResolver.resolve(null).coerceAtMost(studySet.size)
        val targets = studySet.shuffled().take(wanted)

        val generated = coroutineScope {
            targets
                .map { word ->
                    async {
                        word to generator.generate(
                            SentenceRequestBoundary(
                                word = word.text,
                                translation = word.translation,
                                level = level,
                                context = "",
                                requiredWords = emptyList(),
                            ),
                        )
                    }
                }.awaitAll()
        }

        val steps = generated
            .mapNotNull { (word, result) ->
                (result as? SentenceResultBoundary.Generated)?.sentence?.trim()?.takeIf { it.isNotEmpty() }?.let { word to it }
            }.mapIndexed { index, (word, sentence) ->
                PronunciationStepResponse(
                    stepIndex = index,
                    vocabularyItemId = word.id.value,
                    expectedText = sentence,
                    clueText = sentence,
                    transcription = "",
                )
            }
        if (steps.isEmpty()) {
            val results = generated.map { it.second }
            if (results.any { it is SentenceResultBoundary.Offline }) return PronunciationSentencesResult.Offline
            results.firstNotNullOfOrNull { it as? SentenceResultBoundary.Refused }
                ?.let { return PronunciationSentencesResult.Refused(it.reason) }
            return PronunciationSentencesResult.Refused("no sentence came back")
        }

        return PronunciationSentencesResult.Ready(
            PronunciationSessionResponse(sessionId = Uuid.random().toString(), steps = steps),
        )
    }
}

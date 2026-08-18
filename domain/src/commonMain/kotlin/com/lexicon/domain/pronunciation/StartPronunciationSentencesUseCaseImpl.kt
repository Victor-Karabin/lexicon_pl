package com.lexicon.domain.pronunciation

import com.lexicon.boundary.SentenceGenerator
import com.lexicon.boundary.SentenceRequestBoundary
import com.lexicon.boundary.SentenceResultBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.passage.maxLevel
import com.lexicon.interactors.passage.sentenceCountFor
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
) : StartPronunciationSentencesUseCase {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun invoke(): PronunciationSentencesResult {
        val favourites = vocabulary.getItemsByIds(vocabulary.favouriteWordIds())
        if (favourites.isEmpty()) return PronunciationSentencesResult.NoFavourites

        val level = favourites.maxLevel()
        val targets = favourites.shuffled().take(sentenceCountFor(level).random())

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

        generated.firstOrNull { it.second is SentenceResultBoundary.Offline }
            ?.let { return PronunciationSentencesResult.Offline }
        generated.firstOrNull { it.second is SentenceResultBoundary.Refused }
            ?.let { return PronunciationSentencesResult.Refused((it.second as SentenceResultBoundary.Refused).reason) }

        val steps = generated.mapIndexed { index, (word, result) ->
            val sentence = (result as SentenceResultBoundary.Generated).sentence.trim()
            PronunciationStepResponse(
                stepIndex = index,
                vocabularyItemId = word.id,
                // The sentence is both what is shown and what has to be said, so there is
                // nothing to reveal: the exercise is reading it aloud, not recalling it.
                expectedText = sentence,
                clueText = sentence,
                transcription = "",
            )
        }
        if (steps.isEmpty()) return PronunciationSentencesResult.Refused("no sentence came back")

        return PronunciationSentencesResult.Ready(
            PronunciationSessionResponse(sessionId = Uuid.random().toString(), steps = steps),
        )
    }
}

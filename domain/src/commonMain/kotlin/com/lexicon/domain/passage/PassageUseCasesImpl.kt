package com.lexicon.domain.passage

import com.lexicon.boundary.SentenceGenerator
import com.lexicon.boundary.SentenceRequestBoundary
import com.lexicon.boundary.SentenceResultBoundary
import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.boundary.TrainingResultOutcomeBoundary
import com.lexicon.boundary.VocabularyItemBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.common.Clock
import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.interactors.passage.CEFR_ORDER
import com.lexicon.interactors.passage.Passage
import com.lexicon.interactors.passage.PassageSegment
import com.lexicon.interactors.passage.PassageSentence
import com.lexicon.interactors.passage.PassageSessionResult
import com.lexicon.interactors.passage.StartPassageSessionRequest
import com.lexicon.interactors.passage.StartPassageSessionUseCase
import com.lexicon.interactors.passage.SubmitPassageAnswersRequest
import com.lexicon.interactors.passage.SubmitPassageAnswersResponse
import com.lexicon.interactors.passage.SubmitPassageAnswersUseCase
import com.lexicon.interactors.passage.sentenceCountFor
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val TRAINING_ID = "passage"

/** Sentences generated over the target count, to absorb the ones that miss their word. */
private const val SPARE_SENTENCES = 2

/**
 * Below this, a stem is too short to tell words apart — `i` would match everything it
 * begins, so such targets have to appear whole.
 */
private const val MIN_STEM = 4

class StartPassageSessionUseCaseImpl(
    private val vocabulary: VocabularyRepository,
    private val generator: SentenceGenerator,
) : StartPassageSessionUseCase {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun invoke(request: StartPassageSessionRequest): PassageSessionResult {
        val favourites = vocabulary.getItemsByIds(vocabulary.favouriteWordIds())
        if (favourites.isEmpty()) return PassageSessionResult.NoFavourites

        val level = favourites.maxLevel()
        val wanted = sentenceCountFor(level).random()
        val targets = favourites.shuffled().take(wanted + SPARE_SENTENCES)

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
            ?.let { return PassageSessionResult.Offline }
        generated.firstOrNull { it.second is SentenceResultBoundary.Refused }
            ?.let { return PassageSessionResult.Refused((it.second as SentenceResultBoundary.Refused).reason) }

        val sentences = generated.mapNotNull { (word, result) ->
            (result as SentenceResultBoundary.Generated).sentence
                .gapping(word.text)
                ?.let { PassageSentence(it.toImmutableList()) }
        }.take(wanted)
        if (sentences.isEmpty()) return PassageSessionResult.Refused("no sentence used the word it was given")

        val passage = Passage(level = level, sentences = sentences.toImmutableList())
        val answers = passage.gaps.map { it.answer }

        return PassageSessionResult.Ready(
            sessionId = Uuid.random().toString(),
            passage = passage,
            bank = if (!request.withWordBank) {
                emptyList<String>().toImmutableList()
            } else {
                answers.distinct().shuffled(Random(answers.hashCode())).toImmutableList()
            },
        )
    }
}

internal fun List<VocabularyItemBoundary>.maxLevel(): String =
    mapNotNull { it.cefr?.uppercase()?.takeIf { level -> level in CEFR_ORDER } }
        .maxByOrNull { CEFR_ORDER.indexOf(it) }
        ?: CEFR_ORDER.first()

/**
 * Splits a sentence around the word it was written for, or null if that word never
 * turned up in it.
 *
 * The model is told to use the word and mostly does, but now and then it reaches for a
 * synonym — asked for `polegać` it writes `mogę na niego liczyć`. A sentence with
 * nothing to fill in is not an exercise, so it is dropped rather than shown; spares are
 * generated to cover the loss.
 */
private fun String.gapping(target: String): List<PassageSegment>? {
    val stem = target.stem()
    val found = Regex("\\p{L}+").findAll(this).firstOrNull { it.value.grewFrom(stem) } ?: return null

    return buildList {
        if (found.range.first > 0) add(PassageSegment.Text(substring(0, found.range.first)))
        add(PassageSegment.Gap(found.value))
        if (found.range.last + 1 < length) add(PassageSegment.Text(substring(found.range.last + 1)))
    }
}

/**
 * The part of a word that survives Polish inflection, near enough.
 *
 * A phrase is reduced to its longest word, which is the one carrying the meaning:
 * `mieć na myśli` is looked for by `myśli`, not by `mieć`.
 */
private fun String.stem(): String {
    val head = split(' ', '-', '\'').maxByOrNull { it.length }.orEmpty().lowercase()
    return head.take(maxOf(MIN_STEM, head.length * 2 / 3))
}

/** Whether an inflected form in the sentence came from the target, without matching everything. */
private fun String.grewFrom(stem: String): Boolean {
    val word = lowercase()
    return if (stem.length < MIN_STEM) word == stem else word.startsWith(stem)
}

class SubmitPassageAnswersUseCaseImpl(
    private val vocabulary: VocabularyRepository,
    private val history: TrainingHistoryRepository,
    private val answerNormalizer: AnswerNormalizer,
    private val clock: Clock,
) : SubmitPassageAnswersUseCase {
    override suspend fun invoke(request: SubmitPassageAnswersRequest): SubmitPassageAnswersResponse {
        val correct = request.answers.mapIndexed { index, given ->
            answerNormalizer.matches(request.expected.getOrElse(index) { "" }, given)
        }

        correct.forEachIndexed { index, right ->
            val expected = request.expected.getOrElse(index) { "" }
            val word = vocabulary.findWordByText(expected) ?: return@forEachIndexed
            history.recordResult(
                TrainingResultBoundary(
                    sessionId = request.sessionId,
                    trainingType = TRAINING_ID,
                    stepIndex = index,
                    vocabularyItemId = word.id,
                    expectedAnswer = expected,
                    submittedAnswer = request.answers.getOrElse(index) { "" },
                    outcome = if (right) {
                        TrainingResultOutcomeBoundary.CORRECT
                    } else {
                        TrainingResultOutcomeBoundary.INCORRECT
                    },
                    tipUsed = false,
                    completedAtEpochMillis = clock.nowEpochMillis(),
                ),
            )
        }
        return SubmitPassageAnswersResponse(correct = correct)
    }
}

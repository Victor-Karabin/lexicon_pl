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
        val targets = favourites.shuffled().take(wanted)

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

        val segments = mutableListOf<PassageSegment>()
        val answers = mutableListOf<String>()
        generated.forEachIndexed { index, (word, result) ->
            val sentence = (result as SentenceResultBoundary.Generated).sentence
            if (index > 0) segments += PassageSegment.Text(" ")
            segments += sentence.gapping(word.text, answers)
        }

        return PassageSessionResult.Ready(
            sessionId = Uuid.random().toString(),
            passage = Passage(level = level, segments = segments.toImmutableList()),
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

private fun String.gapping(
    target: String,
    answers: MutableList<String>,
): List<PassageSegment> {
    val found = Regex("\\p{L}+").findAll(this).firstOrNull { it.value.startsWithStem(target) }
        ?: return listOf(PassageSegment.Text(this))

    answers += found.value
    return buildList {
        if (found.range.first > 0) add(PassageSegment.Text(substring(0, found.range.first)))
        add(PassageSegment.Gap(found.value))
        if (found.range.last + 1 < length) add(PassageSegment.Text(substring(found.range.last + 1)))
    }
}

private fun String.startsWithStem(target: String): Boolean {
    val stem = target.lowercase().dropLast(if (target.length > 4) 2 else 0)
    return lowercase().startsWith(stem)
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

package com.lexicon.domain.passage

import com.lexicon.boundary.PassageBoundary
import com.lexicon.boundary.PassageRepository
import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.boundary.TrainingResultOutcomeBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.common.Clock
import com.lexicon.interactors.passage.Passage
import com.lexicon.interactors.passage.PassageSegment
import com.lexicon.interactors.passage.PassageSessionResponse
import com.lexicon.interactors.passage.StartPassageSessionRequest
import com.lexicon.interactors.passage.StartPassageSessionUseCase
import com.lexicon.interactors.passage.SubmitPassageAnswersRequest
import com.lexicon.interactors.passage.SubmitPassageAnswersResponse
import com.lexicon.interactors.passage.SubmitPassageAnswersUseCase
import kotlinx.collections.immutable.toImmutableList
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val WORD = Regex("\\p{L}+(?:[-'’]\\p{L}+)*")

private const val TRAINING_ID = "passage"

class StartPassageSessionUseCaseImpl(
    private val passages: PassageRepository,
    private val vocabulary: VocabularyRepository,
) : StartPassageSessionUseCase {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun invoke(request: StartPassageSessionRequest): PassageSessionResponse? {
        val all = passages.passages()
        val chosen = request.passageId?.let { id -> all.firstOrNull { it.id == id } }
            ?: all.randomOrNull()
            ?: return null

        val favourites = vocabulary
            .getItemsByIds(vocabulary.favouriteWordIds())
            .map { it.text.lowercase() }
            .toSet()
        val key = chosen.keyWords.map { it.lowercase() }.toSet()
        val passage = chosen.toPassage(gapWhen = { it in key || it in favourites })

        val answers = passage.gaps.map { it.answer }
        return PassageSessionResponse(
            sessionId = Uuid.random().toString(),
            passage = passage,
            bank = if (!request.withWordBank) {
                emptyList<String>().toImmutableList()
            } else {
                answers.distinct().shuffled(Random(chosen.id.hashCode())).toImmutableList()
            },
        )
    }
}

internal fun PassageBoundary.toPassage(gapWhen: (String) -> Boolean): Passage {
    val segments = mutableListOf<PassageSegment>()
    var at = 0
    WORD.findAll(text).forEach { match ->
        if (!gapWhen(match.value.lowercase())) return@forEach
        if (match.range.first > at) segments += PassageSegment.Text(text.substring(at, match.range.first))
        segments += PassageSegment.Gap(match.value)
        at = match.range.last + 1
    }
    if (at < text.length) segments += PassageSegment.Text(text.substring(at))

    return Passage(
        id = id,
        title = title,
        cefr = cefr,
        segments = segments.toImmutableList(),
    )
}

class SubmitPassageAnswersUseCaseImpl(
    private val passages: PassageRepository,
    private val vocabulary: VocabularyRepository,
    private val history: TrainingHistoryRepository,
    private val clock: Clock,
) : SubmitPassageAnswersUseCase {
    override suspend fun invoke(request: SubmitPassageAnswersRequest): SubmitPassageAnswersResponse {
        val passage = passages.passages().firstOrNull { it.id == request.passageId }
            ?: return SubmitPassageAnswersResponse(correct = request.answers.map { false })

        val expected = request.expected.ifEmpty { passage.keyWords }
        val correct = request.answers.mapIndexed { index, given ->
            given.trim().equals(expected.getOrElse(index) { "" }.trim(), ignoreCase = true)
        }

        correct.forEachIndexed { index, right ->
            val word = vocabulary.findWordByText(expected.getOrElse(index) { "" }) ?: return@forEachIndexed
            history.recordResult(
                TrainingResultBoundary(
                    sessionId = request.sessionId,
                    trainingType = TRAINING_ID,
                    stepIndex = index,
                    vocabularyItemId = word.id,
                    expectedAnswer = expected.getOrElse(index) { "" },
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

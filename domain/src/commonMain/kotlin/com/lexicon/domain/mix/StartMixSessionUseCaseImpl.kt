@file:OptIn(ExperimentalUuidApi::class)

package com.lexicon.domain.mix

import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.interactors.dictation.StartDictationSessionRequest
import com.lexicon.interactors.dictation.StartDictationSessionUseCase
import com.lexicon.interactors.dictationpuzzle.StartDictationPuzzleSessionRequest
import com.lexicon.interactors.dictationpuzzle.StartDictationPuzzleSessionUseCase
import com.lexicon.interactors.imagetest.StartImageTestSessionRequest
import com.lexicon.interactors.imagetest.StartImageTestSessionUseCase
import com.lexicon.interactors.mix.MixSessionResponse
import com.lexicon.interactors.mix.MixStep
import com.lexicon.interactors.mix.MixTrainingType
import com.lexicon.interactors.mix.StartMixSessionRequest
import com.lexicon.interactors.mix.StartMixSessionUseCase
import com.lexicon.interactors.pronunciation.StartPronunciationSessionRequest
import com.lexicon.interactors.pronunciation.StartPronunciationSessionUseCase
import com.lexicon.interactors.puzzle.StartPuzzleSessionRequest
import com.lexicon.interactors.puzzle.StartPuzzleSessionUseCase
import com.lexicon.interactors.trueorfalse.StartTrueOrFalseSessionRequest
import com.lexicon.interactors.trueorfalse.StartTrueOrFalseSessionUseCase
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val MAX_ATTEMPTS_PER_STEP = 12

class StartMixSessionUseCaseImpl(
    private val startDictation: StartDictationSessionUseCase,
    private val startDictationPuzzle: StartDictationPuzzleSessionUseCase,
    private val startPuzzle: StartPuzzleSessionUseCase,
    private val startImageTest: StartImageTestSessionUseCase,
    private val startTrueOrFalse: StartTrueOrFalseSessionUseCase,
    private val startPronunciation: StartPronunciationSessionUseCase,
    private val stepCountResolver: StepCountResolver,
) : StartMixSessionUseCase {
    override suspend fun invoke(request: StartMixSessionRequest): MixSessionResponse {
        val stepCount = stepCountResolver.resolve(request.stepCount)
        val types = request.trainingTypes.ifEmpty { MixTrainingType.entries.toSet() }
        val assignments = assignTrainingTypes(types, stepCount)

        val used = mutableSetOf<Pair<MixTrainingType, Long>>()
        val steps = mutableListOf<MixStep>()
        assignments.forEach { type ->
            distinctStep(
                stepIndex = steps.size,
                type = type,
                used = used,
                vocabularyIds = request.vocabularyIds,
            )?.let { steps += it }
        }

        return MixSessionResponse(sessionId = Uuid.random().toString(), steps = steps)
    }

    private suspend fun distinctStep(
        stepIndex: Int,
        type: MixTrainingType,
        used: MutableSet<Pair<MixTrainingType, Long>>,
        vocabularyIds: List<Long>,
    ): MixStep? {
        repeat(MAX_ATTEMPTS_PER_STEP) {
            val candidate = buildStep(stepIndex, type, vocabularyIds) ?: return null
            if (used.add(type to candidate.vocabularyItemId)) return candidate
        }
        return null
    }

    private fun assignTrainingTypes(
        types: Set<MixTrainingType>,
        stepCount: Int,
    ): List<MixTrainingType> {
        val ordered = types.toList()
        val seeded = ordered.take(stepCount)
        val remainder = List(maxOf(0, stepCount - seeded.size)) { ordered[Random.nextInt(ordered.size)] }
        return (seeded + remainder).shuffled()
    }

    private suspend fun buildStep(
        stepIndex: Int,
        type: MixTrainingType,
        vocabularyIds: List<Long>,
    ): MixStep? =
        when (type) {
            MixTrainingType.DICTATION ->
                startDictation(StartDictationSessionRequest(stepCount = 1, vocabularyIds = vocabularyIds)).steps.firstOrNull()
                    ?.let { MixStep.Dictation(stepIndex, it) }

            MixTrainingType.DICTATION_PUZZLE ->
                startDictationPuzzle(
                    StartDictationPuzzleSessionRequest(stepCount = 1, vocabularyIds = vocabularyIds),
                ).steps.firstOrNull()
                    ?.let { MixStep.DictationPuzzle(stepIndex, it) }

            MixTrainingType.PUZZLE ->
                startPuzzle(StartPuzzleSessionRequest(stepCount = 1, vocabularyIds = vocabularyIds)).steps.firstOrNull()
                    ?.let { MixStep.Puzzle(stepIndex, it) }

            MixTrainingType.IMAGE_TEST ->
                startImageTest(StartImageTestSessionRequest(stepCount = 1, vocabularyIds = vocabularyIds)).steps.firstOrNull()
                    ?.let { MixStep.ImageTest(stepIndex, it) }

            MixTrainingType.TRUE_OR_FALSE ->
                startTrueOrFalse(StartTrueOrFalseSessionRequest(poolSize = 1, vocabularyIds = vocabularyIds)).steps.firstOrNull()
                    ?.let { MixStep.TrueOrFalse(stepIndex, it) }

            MixTrainingType.PRONUNCIATION_CHECK ->
                startPronunciation(StartPronunciationSessionRequest(stepCount = 1, vocabularyIds = vocabularyIds)).steps.firstOrNull()
                    ?.let { MixStep.Pronunciation(stepIndex, it) }
        }
}

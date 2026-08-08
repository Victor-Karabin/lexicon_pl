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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

/**
 * Builds each step by asking the originating training for a one-step session, so generation —
 * distractors, images, letter tiles, true/false pairing — is the training's own logic rather than a
 * reimplementation that could drift from it (Mix spec §9).
 */
class StartMixSessionUseCaseImpl
    @Inject
    constructor(
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

            val steps = coroutineScope {
                assignments.mapIndexed { index, type -> async { buildStep(index, type) } }.awaitAll()
            }.filterNotNull()

            return MixSessionResponse(sessionId = UUID.randomUUID().toString(), steps = steps)
        }

        /**
         * Spec §8: every enabled type should appear at least once when there are enough steps, and
         * the same type shouldn't repeat excessively. Seeding one of each and shuffling gives both,
         * without needing a distribution algorithm the spec leaves open.
         */
        private fun assignTrainingTypes(
            types: Set<MixTrainingType>,
            stepCount: Int,
        ): List<MixTrainingType> {
            val ordered = types.toList()
            val seeded = ordered.take(stepCount)
            val remainder = List(maxOf(0, stepCount - seeded.size)) { ordered[Random.nextInt(ordered.size)] }
            return (seeded + remainder).shuffled()
        }

        /** A type that can't produce a step (e.g. no vocabulary left) is dropped rather than retried — spec §13. */
        private suspend fun buildStep(
            stepIndex: Int,
            type: MixTrainingType,
        ): MixStep? =
            when (type) {
                MixTrainingType.DICTATION ->
                    startDictation(StartDictationSessionRequest(stepCount = 1)).steps.firstOrNull()
                        ?.let { MixStep.Dictation(stepIndex, it) }

                MixTrainingType.DICTATION_PUZZLE ->
                    startDictationPuzzle(StartDictationPuzzleSessionRequest(stepCount = 1)).steps.firstOrNull()
                        ?.let { MixStep.DictationPuzzle(stepIndex, it) }

                MixTrainingType.PUZZLE ->
                    startPuzzle(StartPuzzleSessionRequest(stepCount = 1)).steps.firstOrNull()
                        ?.let { MixStep.Puzzle(stepIndex, it) }

                MixTrainingType.IMAGE_TEST ->
                    startImageTest(StartImageTestSessionRequest(stepCount = 1)).steps.firstOrNull()
                        ?.let { MixStep.ImageTest(stepIndex, it) }

                // poolSize 1: a Mix step is a single question, with none of the standalone
                // training's countdown or answer-as-many-as-you-can pool.
                MixTrainingType.TRUE_OR_FALSE ->
                    startTrueOrFalse(StartTrueOrFalseSessionRequest(poolSize = 1)).steps.firstOrNull()
                        ?.let { MixStep.TrueOrFalse(stepIndex, it) }

                MixTrainingType.PRONUNCIATION_CHECK ->
                    startPronunciation(StartPronunciationSessionRequest(stepCount = 1)).steps.firstOrNull()
                        ?.let { MixStep.Pronunciation(stepIndex, it) }
            }
    }

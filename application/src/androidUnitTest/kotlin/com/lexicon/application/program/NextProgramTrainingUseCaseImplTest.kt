package com.lexicon.application.program

import com.lexicon.interactors.program.AdvanceProgramDayUseCase
import com.lexicon.interactors.program.GetProgramDayUseCase
import com.lexicon.interactors.program.ProgramDay
import com.lexicon.interactors.program.ProgramSession
import com.lexicon.interactors.program.QueuedTraining
import com.lexicon.interactors.program.StartProgramSessionUseCase
import com.lexicon.model.program.ActivityType
import com.lexicon.model.program.ProgramId
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextProgramTrainingUseCaseImplTest {
    private val getDay: GetProgramDayUseCase = mockk()
    private val advanceDay: AdvanceProgramDayUseCase = mockk()
    private val startSession: StartProgramSessionUseCase = mockk()
    private val queue = NextProgramTrainingUseCaseImpl(getDay, advanceDay, startSession)

    private val id = ProgramId("a1")

    private var done = 0
    private lateinit var trainings: List<TrainingType>

    private fun day(): ProgramDay =
        ProgramDay(
            programId = id,
            epochDay = 0,
            queue = trainings
                .mapIndexed { index, training ->
                    QueuedTraining(training = training, round = 0, isDone = index < done)
                }.toImmutableList(),
        )

    private fun given(
        queued: List<TrainingType>,
        wordCount: Int,
    ) {
        trainings = queued
        done = 0
        coEvery { getDay(id) } answers { day() }
        coEvery { advanceDay(id) } answers {
            done = (done + 1).coerceAtMost(trainings.size)
            day()
        }
        coEvery { startSession(id) } returns ProgramSession(
            programId = id,
            activityId = "act",
            activityType = ActivityType.LEARN,
            training = TrainingType.DICTATION.id,
            wordIds = (1..wordCount).map { VocabularyId(it.toLong()) }.toImmutableList(),
        )
    }

    /** The ask: a crossword in the queue must not stall a day with too few words for it. */
    @Test
    fun `a crossword is skipped when the study set is too small for it`() =
        runTest {
            given(listOf(TrainingType.CROSSWORD, TrainingType.DICTATION), wordCount = 2)

            assertEquals(TrainingType.DICTATION, queue.next(id)?.training)
        }

    @Test
    fun `a crossword is offered when there are enough words`() =
        runTest {
            given(listOf(TrainingType.CROSSWORD, TrainingType.DICTATION), wordCount = 30)

            assertEquals(TrainingType.CROSSWORD, queue.next(id)?.training)
        }

    @Test
    fun `several unrunnable trainings in a row are all stepped over`() =
        runTest {
            given(
                listOf(TrainingType.CROSSWORD, TrainingType.WORD_MATCH, TrainingType.IMAGE_TEST, TrainingType.DICTATION),
                wordCount = 1,
            )

            assertEquals(TrainingType.DICTATION, queue.next(id)?.training)
        }

    @Test
    fun `a day of nothing runnable finishes rather than looping`() =
        runTest {
            given(listOf(TrainingType.CROSSWORD, TrainingType.WORD_MATCH), wordCount = 1)

            assertNull(queue.next(id))
        }

    @Test
    fun `advancing marks the current one done before looking further`() =
        runTest {
            given(listOf(TrainingType.DICTATION, TrainingType.PUZZLE), wordCount = 5)

            assertEquals(TrainingType.PUZZLE, queue.advance(id)?.training)
        }

    @Test
    fun `advancing past the last training ends the day`() =
        runTest {
            given(listOf(TrainingType.DICTATION), wordCount = 5)

            assertNull(queue.advance(id))
        }

    @Test
    fun `the words carried forward are the ones the training will get`() =
        runTest {
            given(listOf(TrainingType.DICTATION), wordCount = 4)

            assertEquals(4, queue.next(id)?.wordIds?.size)
        }
}

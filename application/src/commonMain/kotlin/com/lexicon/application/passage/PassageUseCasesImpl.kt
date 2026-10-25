package com.lexicon.application.passage

import com.lexicon.application.dictation.AnswerNormalizer
import com.lexicon.application.settings.StepCountResolver
import com.lexicon.application.training.failure
import com.lexicon.application.training.maxLevel
import com.lexicon.application.training.open
import com.lexicon.application.training.recordOutcome
import com.lexicon.application.training.sentencesFor
import com.lexicon.application.training.wordsToStudy
import com.lexicon.boundary.SentenceGenerator
import com.lexicon.boundary.SentenceResultBoundary
import com.lexicon.boundary.SessionStore
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.passage.Passage
import com.lexicon.interactors.passage.PassageGapResult
import com.lexicon.interactors.passage.PassageSegment
import com.lexicon.interactors.passage.PassageSentence
import com.lexicon.interactors.passage.PassageSessionResult
import com.lexicon.interactors.passage.StartPassageSessionRequest
import com.lexicon.interactors.passage.StartPassageSessionUseCase
import com.lexicon.interactors.passage.SubmitPassageAnswersRequest
import com.lexicon.interactors.passage.SubmitPassageAnswersResponse
import com.lexicon.interactors.passage.SubmitPassageAnswersUseCase
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.Session
import com.lexicon.model.training.SessionId
import com.lexicon.model.training.Step
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType
import kotlinx.collections.immutable.toImmutableList
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi

private const val SPARE_SENTENCES = 2

private const val MIN_STEM = 4

private val WORD = Regex("\\p{L}+")

private val HYPHENATED_WORD = Regex("\\p{L}+(?:-\\p{L}+)+")

class StartPassageSessionUseCaseImpl(
    private val vocabulary: VocabularyRepository,
    private val generator: SentenceGenerator,
    private val stepCountResolver: StepCountResolver,
    private val sessions: SessionStore,
) : StartPassageSessionUseCase {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun invoke(request: StartPassageSessionRequest): PassageSessionResult {
        val studySet = vocabulary.wordsToStudy(request.vocabularyIds)
        if (studySet.isEmpty()) return PassageSessionResult.EmptyStudySet

        val level = studySet.maxLevel()

        val wanted = stepCountResolver.resolve(request.stepCount).coerceAtMost(studySet.size)
        val targets = studySet.shuffled().take(wanted + SPARE_SENTENCES)

        val generated = generator.sentencesFor(targets, level)

        val sentences = generated.mapNotNull { (word, result) ->
            (result as? SentenceResultBoundary.Generated)?.sentence
                ?.gapping(word.text)
                ?.let { PassageSentence(it.toImmutableList()) }
        }.take(wanted)
        if (sentences.isEmpty()) {
            return when (val failure = generated.map { it.second }.failure()) {
                SentenceResultBoundary.Offline -> PassageSessionResult.Offline
                is SentenceResultBoundary.Refused -> PassageSessionResult.Refused(failure.reason)
                else -> PassageSessionResult.Refused("no sentence used the word it was given")
            }
        }

        val passage = Passage(level = level, sentences = sentences.toImmutableList())
        val answers = passage.gaps.map { it.answer }

        val byText = targets.associateBy { it.text }
        val sessionId = sessions.open(
            training = if (request.withWordBank) TrainingType.PASSAGE_BANK else TrainingType.PASSAGE_WRITE,
            answers = passage.gaps.mapNotNull { gap -> byText[gap.word]?.let { it.id to gap.answer } },
        )

        return PassageSessionResult.Ready(
            sessionId = sessionId.value,
            passage = passage,
            bank = if (!request.withWordBank) {
                emptyList<String>().toImmutableList()
            } else {
                answers.distinct().shuffled(Random(answers.hashCode())).toImmutableList()
            },
        )
    }
}

private fun String.gapping(target: String): List<PassageSegment>? {
    val head = target.split(' ', '\'').maxByOrNull { it.length }.orEmpty().lowercase()
    val stem = head.stem()
    val words = if ('-' in head) HYPHENATED_WORD else WORD
    val found = words.findAll(this).firstOrNull { it.value.grewFrom(stem) } ?: return null

    return buildList {
        if (found.range.first > 0) add(PassageSegment.Text(substring(0, found.range.first)))
        add(PassageSegment.Gap(answer = found.value, word = target))
        if (found.range.last + 1 < length) add(PassageSegment.Text(substring(found.range.last + 1)))
    }
}

private fun String.stem(): String {
    val joined = substringBeforeLast('-', missingDelimiterValue = "")
    val last = substringAfterLast('-')
    val prefix = if (joined.isEmpty()) "" else "$joined-"
    return prefix + last.take(maxOf(MIN_STEM, last.length * 2 / 3))
}

private fun String.grewFrom(stem: String): Boolean {
    val word = lowercase()
    return if (stem.length < MIN_STEM) word == stem else word.startsWith(stem)
}

private data class GradedGap(
    val expected: String,
    val wordId: Long?,
    val translation: String,
)

class SubmitPassageAnswersUseCaseImpl(
    private val vocabulary: VocabularyRepository,
    private val recordAnswer: RecordAnswerUseCase,
    private val answerNormalizer: AnswerNormalizer,
    private val sessions: SessionStore,
) : SubmitPassageAnswersUseCase {
    override suspend fun invoke(request: SubmitPassageAnswersRequest): SubmitPassageAnswersResponse {
        val session = sessions.find(SessionId(request.sessionId))
        val training = session?.training ?: TrainingType.PASSAGE_WRITE
        val gaps = session?.let { storedGaps(it) } ?: requestedGaps(request)

        val results = gaps.mapIndexed { index, gap ->
            val submitted = request.answers.getOrElse(index) { "" }
            val right = answerNormalizer.matches(gap.expected, submitted)
            val outcome = if (right) StepOutcome.CORRECT else StepOutcome.INCORRECT

            if (sessions.recordOutcome(request.sessionId, index, outcome) && gap.wordId != null) {
                recordAnswer(
                    RecordedAnswer(
                        sessionId = request.sessionId,
                        trainingType = training,
                        stepIndex = index,
                        vocabularyItemId = gap.wordId,
                        expectedAnswer = gap.expected,
                        submittedAnswer = submitted,
                        outcome = outcome,
                        tipUsed = false,
                    ),
                )
            }

            PassageGapResult(
                expected = gap.expected,
                submitted = submitted,
                translation = gap.translation,
                isCorrect = right,
            )
        }

        return SubmitPassageAnswersResponse(results = results)
    }

    private suspend fun storedGaps(session: Session): List<GradedGap> {
        val questions = session.steps.filterIsInstance<Step.Question>()
        val words = vocabulary.getItemsByIds(questions.map { it.wordId.value }).associateBy { it.id }
        return questions.map { GradedGap(it.expectedAnswer, it.wordId.value, words[it.wordId]?.translation.orEmpty()) }
    }

    private suspend fun requestedGaps(request: SubmitPassageAnswersRequest): List<GradedGap> =
        request.expected.mapIndexed { index, expected ->
            val word = vocabulary.findWordByText(request.words.getOrElse(index) { expected })
            GradedGap(expected, word?.id?.value, word?.translation.orEmpty())
        }
}

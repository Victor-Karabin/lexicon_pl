package com.lexicon.application.passage

import com.lexicon.application.dictation.AnswerNormalizer
import com.lexicon.application.settings.StepCountResolver
import com.lexicon.application.training.open
import com.lexicon.boundary.SentenceGenerator
import com.lexicon.boundary.SentenceRequestBoundary
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
import com.lexicon.model.training.SessionId
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.CefrLevel
import com.lexicon.model.vocabulary.Word
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi

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
    private val stepCountResolver: StepCountResolver,
    private val sessions: SessionStore,
) : StartPassageSessionUseCase {
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun invoke(request: StartPassageSessionRequest): PassageSessionResult {
        val studySet = vocabulary.getItemsByIds(vocabulary.studySetWordIds())
        if (studySet.isEmpty()) return PassageSessionResult.EmptyStudySet

        val level = studySet.maxLevel()

        // One gap per step, so the setting that governs how long every other training runs
        // governs this one too. The level still decides how hard the sentences are; it no
        // longer decides how many there are.
        val wanted = stepCountResolver.resolve(request.stepCount).coerceAtMost(studySet.size)
        val targets = studySet.shuffled().take(wanted + SPARE_SENTENCES)

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

internal fun List<Word>.maxLevel(): String = mapNotNull { it.cefr }.maxByOrNull { it.ordinal }?.name ?: CefrLevel.A1.name

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
        add(PassageSegment.Gap(answer = found.value, word = target))
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
    private val recordAnswer: RecordAnswerUseCase,
    private val answerNormalizer: AnswerNormalizer,
    private val sessions: SessionStore,
) : SubmitPassageAnswersUseCase {
    override suspend fun invoke(request: SubmitPassageAnswersRequest): SubmitPassageAnswersResponse {
        val session = sessions.find(SessionId(request.sessionId))
        val training = session?.training ?: TrainingType.PASSAGE_WRITE

        val results = request.expected.mapIndexed { index, expected ->
            val submitted = request.answers.getOrElse(index) { "" }
            val right = answerNormalizer.matches(expected, submitted)
            val word = vocabulary.findWordByText(request.words.getOrElse(index) { expected })

            if (word != null) {
                recordAnswer(
                    RecordedAnswer(
                        sessionId = request.sessionId,
                        trainingType = training,
                        stepIndex = index,
                        vocabularyItemId = word.id.value,
                        expectedAnswer = expected,
                        submittedAnswer = submitted,
                        outcome = if (right) {
                            StepOutcome.CORRECT
                        } else {
                            StepOutcome.INCORRECT
                        },
                        tipUsed = false,
                    ),
                )
            }

            PassageGapResult(
                expected = expected,
                submitted = submitted,
                translation = word?.translation.orEmpty(),
                isCorrect = right,
            )
        }

        return SubmitPassageAnswersResponse(results = results)
    }
}

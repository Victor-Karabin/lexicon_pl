package com.lexicon.domain.program

import com.lexicon.boundary.ProgramBoundary
import com.lexicon.boundary.ProgramRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.common.Clock
import com.lexicon.interactors.presets.LocalizedText
import com.lexicon.interactors.program.ActivityConfig
import com.lexicon.interactors.program.ActivityType
import com.lexicon.interactors.program.CountFavouritesUseCase
import com.lexicon.interactors.program.CreateProgramUseCase
import com.lexicon.interactors.program.DailyPlanConfig
import com.lexicon.interactors.program.Program
import com.lexicon.interactors.program.ProgramConfig
import com.lexicon.interactors.program.ProgramDifficulty
import com.lexicon.interactors.program.ProgramDraft
import com.lexicon.interactors.program.ProgramDraftException
import com.lexicon.interactors.program.ProgramDraftProblem
import com.lexicon.interactors.program.ProgramGoal
import com.lexicon.interactors.program.ProgramId
import com.lexicon.interactors.program.ProgramVisibility
import com.lexicon.interactors.program.ProgressWeights
import com.lexicon.interactors.program.ScopeOrdering
import com.lexicon.interactors.program.ScopeSource
import com.lexicon.interactors.program.ScopeSourceType
import com.lexicon.interactors.program.TargetType
import com.lexicon.interactors.program.UpdateProgramUseCase
import com.lexicon.interactors.program.VocabularyScope
import kotlinx.serialization.json.Json

/**
 * A program of the learner's own is still a program: it is stored as configuration
 * and run by the same engine, so nothing downstream needs to know who wrote it.
 */
private val configJson = Json { encodeDefaults = true }

/** Only what the form asked about is chosen; the rest follows from the study set. */
private const val VOCABULARY_WEIGHT = 60
private const val ACCURACY_WEIGHT = 40
private const val ACCURACY_TARGET = 90

/** Own programs sort above the shipped ones: the learner wrote them, they matter more. */
private const val USER_PROGRAM_ORDER = 0

class CreateProgramUseCaseImpl(
    programs: ProgramRepository,
    vocabulary: VocabularyRepository,
    private val clock: Clock,
) : CreateProgramUseCase {
    private val writer = ProgramWriter(programs, vocabulary)

    // A fresh id, unique without a lookup and sorted by when it was made.
    override suspend fun invoke(draft: ProgramDraft): Result<Program> = writer.write(ProgramId("user-${clock.nowEpochMillis()}"), draft)
}

/**
 * Rewrites one in place. Same id, so the enrolment and the days recorded against it
 * are still about this program afterwards.
 */
class UpdateProgramUseCaseImpl(
    programs: ProgramRepository,
    vocabulary: VocabularyRepository,
) : UpdateProgramUseCase {
    private val writer = ProgramWriter(programs, vocabulary)

    override suspend fun invoke(
        id: ProgramId,
        draft: ProgramDraft,
    ): Result<Program> = writer.write(id, draft)
}

/** Turning a draft into a program, which is the same work whether it is new or not. */
private class ProgramWriter(
    private val programs: ProgramRepository,
    private val vocabulary: VocabularyRepository,
) {
    suspend fun write(
        id: ProgramId,
        draft: ProgramDraft,
    ): Result<Program> {
        val title = draft.title.trim()
        if (title.isEmpty()) return failure(ProgramDraftProblem.MISSING_TITLE)
        if (draft.trainings.isEmpty()) return failure(ProgramDraftProblem.NO_TRAININGS)

        val favourites = vocabulary.favouriteWordIds().size
        if (favourites == 0) return failure(ProgramDraftProblem.NO_FAVOURITES)

        val config = draft.toConfig(favourites)
        val program = Program(
            id = id,
            level = "",
            order = USER_PROGRAM_ORDER,
            title = LocalizedText(mapOf(LocalizedText.DEFAULT_LANGUAGE to title)),
            description = LocalizedText(mapOf(LocalizedText.DEFAULT_LANGUAGE to draft.description.trim())),
            difficulty = ProgramDifficulty.BEGINNER,
            estimatedDays = 0,
            visibility = ProgramVisibility.PRIVATE,
            config = config,
        )

        programs.saveProgram(
            ProgramBoundary(
                id = program.id.value,
                level = program.level,
                order = program.order,
                title = program.title.values,
                description = program.description.values,
                difficulty = program.difficulty.name,
                estimatedDays = program.estimatedDays,
                visibility = program.visibility.name,
                configJson = configJson.encodeToString(ProgramConfig.serializer(), config),
            ),
        )
        return Result.success(program)
    }

    /**
     * The rest of the program, worked out rather than asked for.
     *
     * The goal is the study set itself: a program over favourites is finished when
     * the learner knows the words they starred, so the target is however many that
     * is. The draft's list becomes the day's queue as written — a training twice in
     * it is a training worked through twice — and the activities name the distinct
     * ones so reviews and new words both have somewhere to run.
     */
    private fun ProgramDraft.toConfig(favourites: Int): ProgramConfig =
        ProgramConfig(
            goals = listOf(
                ProgramGoal(id = "words", type = TargetType.VOCABULARY, target = favourites),
                ProgramGoal(id = "accuracy", type = TargetType.ACCURACY, target = ACCURACY_TARGET, required = false),
            ),
            scope = VocabularyScope(
                include = listOf(ScopeSource(type = ScopeSourceType.FAVOURITES)),
                ordering = ScopeOrdering.FREQUENCY,
            ),
            dailyPlan = DailyPlanConfig(
                newWords = newWordsPerDay.coerceIn(0, favourites),
                reviewWords = reviewWordsPerDay.coerceIn(0, favourites),
                queue = trainings,
                activities = listOf(
                    ActivityConfig(
                        id = "learn",
                        type = ActivityType.LEARN,
                        target = newWordsPerDay,
                        priority = 1,
                        trainings = trainings.distinct(),
                    ),
                    ActivityConfig(
                        id = "review",
                        type = ActivityType.REVIEW,
                        target = reviewWordsPerDay,
                        priority = 2,
                        trainings = trainings.distinct(),
                    ),
                ),
            ),
            progress = ProgressWeights(
                vocabulary = VOCABULARY_WEIGHT,
                milestones = 0,
                accuracy = ACCURACY_WEIGHT,
            ),
        )

    private fun failure(problem: ProgramDraftProblem): Result<Program> = Result.failure(ProgramDraftException(problem))
}

class CountFavouritesUseCaseImpl(
    private val vocabulary: VocabularyRepository,
) : CountFavouritesUseCase {
    override suspend fun invoke(): Int = vocabulary.favouriteWordIds().size
}

package com.lexicon.application.program

import com.lexicon.boundary.ProgramRepository
import com.lexicon.boundary.ReviewScheduleRepository
import com.lexicon.boundary.StudyRecordRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.common.Clock
import com.lexicon.interactors.program.GetProgramDayUseCase
import com.lexicon.interactors.program.GetProgramProgressUseCase
import com.lexicon.interactors.program.GetProgramUseCase
import com.lexicon.interactors.program.Program
import com.lexicon.interactors.program.ProgramSession
import com.lexicon.interactors.program.StartProgramSessionUseCase
import com.lexicon.model.program.ActivityType
import com.lexicon.model.program.ProgramId
import com.lexicon.model.program.ProgramProgress
import com.lexicon.model.program.ProgressMetric
import com.lexicon.model.program.ProgressMetricType
import com.lexicon.model.program.TargetType
import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.toImmutableList

private const val PERCENT = 100

class StartProgramSessionUseCaseImpl(
    private val getProgram: GetProgramUseCase,
    private val getDay: GetProgramDayUseCase,
    private val vocabulary: VocabularyRepository,
) : StartProgramSessionUseCase {
    override suspend fun invoke(id: ProgramId): ProgramSession? {
        val program = getProgram(id) ?: return null
        val plan = program.config.dailyPlan

        val learning = getDay(id)?.newWords?.map { it.value }.orEmpty()
        val known = vocabulary
            .randomKnownWordIds(plan.reviewWords.coerceAtLeast(0))
            .filterNot { it in learning }

        val words = (learning + known).shuffled().ifEmpty { return null }
        val wanted = if (learning.isEmpty()) ActivityType.REVIEW else ActivityType.LEARN
        val activity = plan.activities.firstOrNull { it.type == wanted }
            ?: plan.activities.firstOrNull()
            ?: return null

        return activity.session(program, words)
    }

    private fun Int.orAll(): Int = if (this <= 0) Int.MAX_VALUE else this

    private fun com.lexicon.interactors.program.ActivityConfig.session(
        program: Program,
        wordIds: List<Long>,
    ): ProgramSession? {
        val training = trainings.firstOrNull() ?: return null
        return ProgramSession(
            programId = program.id,
            activityId = id,
            activityType = type,
            training = training,
            wordIds = wordIds.map(::VocabularyId).toImmutableList(),
        )
    }
}

class GetProgramProgressUseCaseImpl(
    private val programs: ProgramRepository,
    private val reviews: ReviewScheduleRepository,
    private val study: StudyRecordRepository,
    private val clock: Clock,
) : GetProgramProgressUseCase {
    override suspend fun invoke(program: Program): ProgramProgress {
        val weights = program.config.progress
        val metrics = mutableListOf<ProgressMetric>()

        if (weights.vocabulary > 0) {
            val target = program.config.goals
                .firstOrNull { it.type == TargetType.VOCABULARY }
                ?.target
                ?: 0
            metrics += ProgressMetric(
                type = ProgressMetricType.VOCABULARY,
                current = reviews.countMastered(program.config.review.masteredIntervalDays.toLong()),
                target = target,
                weight = weights.vocabulary,
            )
        }

        if (weights.milestones > 0) {
            metrics += ProgressMetric(
                type = ProgressMetricType.MILESTONES,
                current = programs.milestones(program.id.value).size,
                target = program.config.milestones.size,
                weight = weights.milestones,
            )
        }

        if (weights.accuracy > 0) {
            val today = study.day(clock.todayEpochDay())
            val answers = today?.answers ?: 0
            metrics += ProgressMetric(
                type = ProgressMetricType.ACCURACY,
                current = if (answers == 0) 0 else (today!!.correctAnswers * PERCENT) / answers,
                target = PERCENT,
                weight = weights.accuracy,
                isMeasured = answers > 0,
            )
        }

        if (weights.consistency > 0) {
            val today = clock.todayEpochDay()
            val studiedDays = study.daysBetween(0, today).filter { it.answers > 0 }
            val elapsed = studiedDays.minOfOrNull { it.epochDay }?.let { (today - it + 1).toInt() } ?: 0

            metrics += ProgressMetric(
                type = ProgressMetricType.CONSISTENCY,
                current = studiedDays.size,
                target = elapsed.coerceAtLeast(1),
                weight = weights.consistency,
            )
        }

        return ProgramProgress(programId = program.id, metrics = metrics.toImmutableList())
    }
}

class GetStudyStreakUseCaseImpl(
    private val study: StudyRecordRepository,
    private val clock: Clock,
) : com.lexicon.interactors.program.GetStudyStreakUseCase {
    override suspend fun invoke(): Int = study.currentStreak(clock.todayEpochDay())
}

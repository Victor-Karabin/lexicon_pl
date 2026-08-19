package com.lexicon.application.program

import com.lexicon.boundary.CourseRepository
import com.lexicon.boundary.ProgramRepository
import com.lexicon.boundary.ReviewScheduleRepository
import com.lexicon.boundary.StudyRecordRepository
import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.common.Clock
import com.lexicon.interactors.program.GetProgramProgressUseCase
import com.lexicon.interactors.program.GetProgramUseCase
import com.lexicon.interactors.program.Program
import com.lexicon.interactors.program.ProgramSession
import com.lexicon.interactors.program.ResolveProgramScopeUseCase
import com.lexicon.interactors.program.StartProgramSessionUseCase
import com.lexicon.model.program.ActivityType
import com.lexicon.model.program.ProgramId
import com.lexicon.model.program.ProgramProgress
import com.lexicon.model.program.ProgressMetric
import com.lexicon.model.program.ProgressMetricType
import com.lexicon.model.program.ScopeSourceType
import com.lexicon.model.program.TargetType
import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private const val PERCENT = 100

class ResolveProgramScopeUseCaseImpl(
    private val vocabulary: VocabularyRepository,
    private val presets: VocabularyPresetRepository,
    private val courses: CourseRepository,
) : ResolveProgramScopeUseCase {
    override suspend fun invoke(program: Program): ImmutableList<VocabularyId> {
        val scope = program.config.scope

        val included = linkedSetOf<Long>()
        scope.include.forEach { included += wordIdsOf(it.type, it.value) }
        scope.exclude.forEach { included -= wordIdsOf(it.type, it.value).toSet() }

        val byId = vocabulary.getItemsByIds(included.toList()).associateBy { it.id.value }
        val ordered = scope.ordering.applyTo(included.mapNotNull { byId[it] })

        val cap = scope.maxWords
        return ordered
            .let { if (cap != null) it.take(cap) else it }
            .map { it.id }
            .toImmutableList()
    }

    private suspend fun wordIdsOf(
        type: ScopeSourceType,
        value: String,
    ): List<Long> =
        when (type) {
            ScopeSourceType.PRESET -> presets.getPreset(value)?.vocabularyIds.orEmpty()
            ScopeSourceType.STUDY_SET -> vocabulary.studySetWordIds()
            ScopeSourceType.CEFR_LEVEL -> vocabulary.wordIdsForLevel(value)
            ScopeSourceType.ALL -> vocabulary.allWordIds()

            ScopeSourceType.LESSON -> courses.getLessonWordIds(value)
        }
}

class StartProgramSessionUseCaseImpl(
    private val getProgram: GetProgramUseCase,
    private val resolveScope: ResolveProgramScopeUseCase,
    private val reviews: ReviewScheduleRepository,
    private val clock: Clock,
) : StartProgramSessionUseCase {
    override suspend fun invoke(id: ProgramId): ProgramSession? {
        val program = getProgram(id) ?: return null
        val plan = program.config.dailyPlan
        val scope = resolveScope(program).map { it.value }
        if (scope.isEmpty()) return null

        val inScope = scope.toSet()
        val due = reviews
            .dueWordIds(clock.todayEpochDay(), program.config.review.dailyLimit)
            .filter { it in inScope }

        val reviewActivity = plan.activities.firstOrNull { it.type == ActivityType.REVIEW }
        if (due.isNotEmpty() && reviewActivity != null) {
            return reviewActivity.session(program, due.take(plan.reviewWords.orAll()))
        }

        val learnActivity = plan.activities.firstOrNull { it.type == ActivityType.LEARN }
            ?: plan.activities.firstOrNull()
            ?: return null
        val met = reviews.scheduledWordIds()
        val fresh = scope.filterNot { it in met }.take(plan.newWords.orAll())

        val words = fresh.ifEmpty { return null }
        return learnActivity.session(program, words)
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
            val enrolment = programs.enrolment(program.id.value)
            val today = clock.todayEpochDay()

            val elapsed = enrolment?.let { (today - it.startedAtEpochDay + 1).toInt() } ?: 0
            val studied = enrolment?.let {
                study.daysBetween(it.startedAtEpochDay, today).count { day -> day.answers > 0 }
            } ?: 0
            metrics += ProgressMetric(
                type = ProgressMetricType.CONSISTENCY,
                current = studied,
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

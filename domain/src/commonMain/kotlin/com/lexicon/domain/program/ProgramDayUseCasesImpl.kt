package com.lexicon.domain.program

import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.ProgramDayBoundary
import com.lexicon.boundary.ProgramRepository
import com.lexicon.boundary.ReviewScheduleRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.common.Clock
import com.lexicon.interactors.program.AdvanceProgramDayUseCase
import com.lexicon.interactors.program.GetProgramDayUseCase
import com.lexicon.interactors.program.GetProgramUseCase
import com.lexicon.interactors.program.GetWordCardsUseCase
import com.lexicon.interactors.program.MarkCardsSeenUseCase
import com.lexicon.interactors.program.Program
import com.lexicon.interactors.program.ProgramDay
import com.lexicon.interactors.program.ProgramId
import com.lexicon.interactors.program.QueuedTraining
import com.lexicon.interactors.program.ResolveProgramScopeUseCase
import com.lexicon.interactors.program.WordCard
import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val MILLIS_PER_DAY = 86_400_000L

private val dayJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class StoredDay(
    val newWords: List<Long> = emptyList(),
    val cardsSeen: Boolean = false,
    val done: Int = 0,
    val queueFingerprint: String = "",
)

private fun List<String>.fingerprint(): String = joinToString("|")

class GetProgramDayUseCaseImpl(
    private val getProgram: GetProgramUseCase,
    private val resolveScope: ResolveProgramScopeUseCase,
    private val programs: ProgramRepository,
    private val reviews: ReviewScheduleRepository,
    private val clock: Clock,
) : GetProgramDayUseCase {
    override suspend fun invoke(id: ProgramId): ProgramDay? {
        val program = getProgram(id) ?: return null
        val today = clock.todayEpochDay()

        val queue = program.config.dailyPlan.queue
        val fingerprint = queue.fingerprint()

        val loaded = programs.day(id.value, today)?.let {
            runCatching { dayJson.decodeFromString(StoredDay.serializer(), it.activitiesJson) }.getOrNull()
        }
        val stored = when {
            loaded == null -> generate(program, fingerprint).also { save(id, today, it, queue.size) }
            loaded.queueFingerprint != fingerprint ->
                loaded.copy(done = 0, queueFingerprint = fingerprint).also { save(id, today, it, queue.size) }
            else -> loaded
        }

        return ProgramDay(
            programId = id,
            epochDay = today,
            newWords = stored.newWords.map(::VocabularyId).toImmutableList(),
            cardsSeen = stored.cardsSeen,
            queue = queue.toQueue(stored.done),
        )
    }

    private suspend fun generate(
        program: Program,
        fingerprint: String,
    ): StoredDay {
        val plan = program.config.dailyPlan
        val scope = resolveScope(program).map { it.value }
        val met = reviews.scheduledWordIds()
        val newWords = scope
            .filterNot { it in met }
            .take(plan.newWords.coerceAtLeast(0))

        return StoredDay(
            newWords = newWords,
            cardsSeen = newWords.isEmpty(),
            queueFingerprint = fingerprint,
        )
    }

    private suspend fun save(
        id: ProgramId,
        today: Long,
        day: StoredDay,
        turns: Int,
    ) {
        programs.saveDay(
            ProgramDayBoundary(
                programId = id.value,
                epochDay = today,
                activitiesJson = dayJson.encodeToString(StoredDay.serializer(), day),
                appliedRulesJson = "[]",
                isComplete = turns > 0 && day.done >= turns,
            ),
        )
    }

    private fun List<String>.toQueue(done: Int): ImmutableList<QueuedTraining> {
        val taken = mutableMapOf<String, Int>()
        return mapIndexed { index, training ->
            val round = taken.getOrElse(training) { 0 }
            taken[training] = round + 1
            QueuedTraining(training = training, round = round, isDone = index < done)
        }.toImmutableList()
    }
}

class AdvanceProgramDayUseCaseImpl(
    private val getDay: GetProgramDayUseCase,
    private val programs: ProgramRepository,
    private val clock: Clock,
) : AdvanceProgramDayUseCase {
    override suspend fun invoke(id: ProgramId): ProgramDay? {
        val day = getDay(id) ?: return null
        val today = clock.todayEpochDay()
        val existing = programs.day(id.value, today) ?: return day
        val stored = runCatching {
            dayJson.decodeFromString(StoredDay.serializer(), existing.activitiesJson)
        }.getOrNull() ?: return day

        val turns = day.queue.size
        val done = (stored.done + 1).coerceAtMost(turns)
        programs.saveDay(
            existing.copy(
                activitiesJson = dayJson.encodeToString(StoredDay.serializer(), stored.copy(done = done)),
                isComplete = turns > 0 && done >= turns,
            ),
        )
        return getDay(id)
    }
}

class MarkCardsSeenUseCaseImpl(
    private val programs: ProgramRepository,
    private val clock: Clock,
) : MarkCardsSeenUseCase {
    override suspend fun invoke(id: ProgramId) {
        val today = clock.todayEpochDay()
        val existing = programs.day(id.value, today) ?: return
        val stored = runCatching { dayJson.decodeFromString(StoredDay.serializer(), existing.activitiesJson) }.getOrNull() ?: return
        programs.saveDay(existing.copy(activitiesJson = dayJson.encodeToString(StoredDay.serializer(), stored.copy(cardsSeen = true))))
    }
}

class GetWordCardsUseCaseImpl(
    private val vocabulary: VocabularyRepository,
    private val imageProvider: ImageProvider,
) : GetWordCardsUseCase {
    override suspend fun invoke(ids: List<VocabularyId>): ImmutableList<WordCard> {
        if (ids.isEmpty()) return emptyList<WordCard>().toImmutableList()
        val words = vocabulary.getItemsByIds(ids.map { it.value }).associateBy { it.id.value }

        return ids
            .mapNotNull { words[it.value] }
            .map { word ->
                WordCard(
                    id = word.id,
                    text = word.text,
                    translation = word.translation,
                    transcription = word.transcription,
                    imageUrl = runCatching { imageProvider.searchImage(word.translation) }.getOrNull(),
                )
            }.toImmutableList()
    }
}

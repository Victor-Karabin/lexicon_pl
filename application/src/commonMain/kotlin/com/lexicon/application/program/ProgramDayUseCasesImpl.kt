package com.lexicon.application.program

import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.ProgramDayBoundary
import com.lexicon.boundary.ProgramRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.common.Clock
import com.lexicon.interactors.program.AdvanceProgramDayUseCase
import com.lexicon.interactors.program.GetProgramDayUseCase
import com.lexicon.interactors.program.GetProgramUseCase
import com.lexicon.interactors.program.GetWordCardsUseCase
import com.lexicon.interactors.program.MarkCardsSeenUseCase
import com.lexicon.interactors.program.ProgramDay
import com.lexicon.interactors.program.QueuedTraining
import com.lexicon.interactors.program.WordCard
import com.lexicon.model.program.ProgramId
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val dayJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class StoredDay(
    val cardsSeen: Boolean = false,
    val done: Int = 0,
    val queueFingerprint: String = "",
)

private fun fingerprintOf(
    queue: List<String>,
    newWordsADay: Int,
): String = "${queue.joinToString("|")}#$newWordsADay"

class GetProgramDayUseCaseImpl(
    private val getProgram: GetProgramUseCase,
    private val programs: ProgramRepository,
    private val vocabulary: VocabularyRepository,
    private val clock: Clock,
) : GetProgramDayUseCase {
    override suspend fun invoke(id: ProgramId): ProgramDay? {
        val program = getProgram(id) ?: return null
        val today = clock.todayEpochDay()

        val queue = program.config.dailyPlan.queue
        val fingerprint = fingerprintOf(queue, program.config.dailyPlan.newWords)

        val loaded = programs.day(id.value, today)?.let {
            runCatching { dayJson.decodeFromString(StoredDay.serializer(), it.activitiesJson) }.getOrNull()
        }
        val stored = when (loaded?.queueFingerprint) {
            fingerprint -> loaded
            else -> generate(fingerprint).also { save(id, today, it, queue.size) }
        }

        val learning = vocabulary.learningWordIds(program.config.dailyPlan.newWords.coerceAtLeast(0))

        return ProgramDay(
            programId = id,
            epochDay = today,
            newWords = learning.map(::VocabularyId).toImmutableList(),
            cardsSeen = stored.cardsSeen,
            queue = queue.toQueue(stored.done),
        )
    }

    private fun generate(fingerprint: String): StoredDay = StoredDay(queueFingerprint = fingerprint)

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
        val taken = mutableMapOf<TrainingType, Int>()
        return mapIndexedNotNull { index, id ->
            val training = TrainingType.ofId(id) ?: return@mapIndexedNotNull null
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
                    example = word.example,
                )
            }.toImmutableList()
    }
}

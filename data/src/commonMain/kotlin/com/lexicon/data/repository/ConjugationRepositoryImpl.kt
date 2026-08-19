package com.lexicon.data.repository

import com.lexicon.boundary.ConjugationCourseBoundary
import com.lexicon.boundary.ConjugationProgressBoundary
import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.SyncOutcomeBoundary
import com.lexicon.boundary.VerbConjugationBoundary
import com.lexicon.common.Clock
import com.lexicon.data.local.ConjugationAssetLoader
import com.lexicon.data.local.ConjugationCourseEntity
import com.lexicon.data.local.ConjugationCourseVerbEntity
import com.lexicon.data.local.ConjugationDao
import com.lexicon.data.local.ConjugationProgressEntity
import com.lexicon.data.local.ConjugationVerbEntity
import com.lexicon.data.local.VocabularySyncStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val formsSerializer = MapSerializer(String.serializer(), ListSerializer(String.serializer()))

class ConjugationRepositoryImpl(
    private val loader: ConjugationAssetLoader,
    private val dao: ConjugationDao,
    private val syncStore: VocabularySyncStore,
    private val clock: Clock,
) : ConjugationRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private var cachedAsset: List<VerbConjugationBoundary>? = null

    override suspend fun syncFromSource(): SyncOutcomeBoundary =
        lock.withLock {
            val fingerprint = loader.fingerprint()

            if (fingerprint == syncStore.syncedVerbFingerprint() && dao.countVerbs() > 0) {
                return@withLock SyncOutcomeBoundary(total = dao.countVerbs(), added = 0, updated = 0, removed = 0)
            }

            val before = dao.countVerbs()
            seed()
            syncStore.setSyncedVerbFingerprint(fingerprint)

            SyncOutcomeBoundary(total = dao.countVerbs(), added = (dao.countVerbs() - before).coerceAtLeast(0), updated = 0, removed = 0)
        }

    override suspend fun countVerbs(): Int = dao.countVerbs()

    override suspend fun verbs(): List<VerbConjugationBoundary> = dao.verbs().map { it.toBoundary() }

    override suspend fun deleteVerb(infinitive: String) = dao.deleteVerb(infinitive)

    /** Fewer verbs than the file holds means some were deleted; nothing else records it. */
    override suspend fun hasDeletedVerbs(): Boolean = dao.countVerbs() < assetVerbs().size

    override suspend fun restoreVerbs() = lock.withLock { seed() }

    override suspend fun courses(): List<ConjugationCourseBoundary> =
        dao.courses().map { ConjugationCourseBoundary(id = it.id, infinitives = dao.courseVerbs(it.id)) }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun createCourse(infinitives: List<String>): String {
        val id = Uuid.random().toString()
        dao.saveCourse(ConjugationCourseEntity(id = id, createdAtEpochMillis = clock.nowEpochMillis()))
        dao.addCourseVerbs(infinitives.distinct().map { ConjugationCourseVerbEntity(id, it) })
        return id
    }

    override suspend fun deleteCourse(courseId: String) {
        dao.clearProgress(courseId)
        dao.clearCourseVerbs(courseId)
        dao.deleteCourse(courseId)
    }

    override suspend fun progress(courseId: String): List<ConjugationProgressBoundary> =
        dao.progress(courseId).map {
            ConjugationProgressBoundary(
                infinitive = it.infinitive,
                person = it.person,
                attempted = it.attempted,
                correct = it.correct,
                incorrect = it.incorrect,
                streak = it.streak,
            )
        }

    override suspend fun recordAttempt(
        courseId: String,
        infinitive: String,
        person: String,
        isCorrect: Boolean,
    ) {
        val existing = dao.progressFor(courseId, infinitive, person)
        dao.save(
            ConjugationProgressEntity(
                courseId = courseId,
                infinitive = infinitive,
                person = person,
                attempted = (existing?.attempted ?: 0) + 1,
                correct = (existing?.correct ?: 0) + if (isCorrect) 1 else 0,
                incorrect = (existing?.incorrect ?: 0) + if (isCorrect) 0 else 1,
                streak = if (isCorrect) (existing?.streak ?: 0) + 1 else 0,
            ),
        )
    }

    private suspend fun assetVerbs(): List<VerbConjugationBoundary> = cachedAsset ?: loader.load().also { cachedAsset = it }

    private suspend fun seed() {
        dao.saveVerbs(
            assetVerbs().map { verb ->
                ConjugationVerbEntity(
                    infinitive = verb.infinitive,
                    translation = verb.translation.orEmpty(),
                    formsJson = json.encodeToString(formsSerializer, verb.forms),
                )
            },
        )
    }

    private fun ConjugationVerbEntity.toBoundary(): VerbConjugationBoundary =
        VerbConjugationBoundary(
            infinitive = infinitive,
            translation = translation.takeIf { it.isNotBlank() },
            forms = runCatching { json.decodeFromString(formsSerializer, formsJson) }.getOrDefault(emptyMap()),
        )
}

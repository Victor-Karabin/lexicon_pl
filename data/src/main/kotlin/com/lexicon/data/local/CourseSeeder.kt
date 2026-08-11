package com.lexicon.data.local

import com.lexicon.boundary.SyncOutcomeBoundary
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseSeeder
    @Inject
    constructor(
        private val courseDao: CourseDao,
        private val loader: CourseAssetLoader,
        private val syncStore: VocabularySyncStore,
    ) {
        private val mutex = Mutex()

        @Volatile
        private var syncedThisProcess = false

        suspend fun ensureSeeded() {
            if (syncedThisProcess) return
            sync()
        }

        suspend fun sync(): SyncOutcomeBoundary =
            mutex.withLock {
                val fingerprint = loader.fingerprint()
                val stored = courseDao.countLessons()
                if (fingerprint == syncStore.syncedCourseFingerprint() && stored > 0) {
                    syncedThisProcess = true
                    return@withLock SyncOutcomeBoundary(total = stored, added = 0, updated = 0, removed = 0)
                }

                val catalog = loader.load()
                val lessons = catalog.courses.flatMap { it.lessons }
                courseDao.replaceCatalog(
                    courses = catalog.courses.map { it.toEntity() },
                    lessons = lessons.map { it.toEntity() },
                    words = lessons.flatMap { it.toWordEntities() },
                    audio = lessons.flatMap { it.toAudioEntities() },
                    exercises = lessons.flatMap { it.toExerciseEntities() },
                    exerciseItems = lessons.flatMap { it.toExerciseItemEntities() },
                )
                syncStore.setSyncedCourseFingerprint(fingerprint)
                syncedThisProcess = true

                SyncOutcomeBoundary(
                    total = lessons.size,
                    added = lessons.size - stored.coerceAtMost(lessons.size),
                    updated = stored.coerceAtMost(lessons.size),
                    removed = (stored - lessons.size).coerceAtLeast(0),
                )
            }
    }

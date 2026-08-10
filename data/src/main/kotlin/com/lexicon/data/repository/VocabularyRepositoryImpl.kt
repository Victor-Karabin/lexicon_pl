package com.lexicon.data.repository

import com.lexicon.boundary.SyncOutcomeBoundary
import com.lexicon.boundary.VocabularyItemBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.data.local.VocabularySeeder
import com.lexicon.data.local.WordDao
import com.lexicon.data.local.toBoundary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class VocabularyRepositoryImpl
    @Inject
    constructor(
        private val wordDao: WordDao,
        private val vocabularySeeder: VocabularySeeder,
    ) : VocabularyRepository {
        override suspend fun getRandomItems(
            count: Int,
            restrictToIds: List<Long>,
        ): List<VocabularyItemBoundary> {
            vocabularySeeder.ensureSeeded()
            val words = if (restrictToIds.isEmpty()) {
                wordDao.getRandomForStudy(count)
            } else {
                wordDao.getRandomFromIds(restrictToIds, count)
            }
            return words.map { it.toBoundary() }
        }

        override suspend fun getItemsByIds(ids: List<Long>): List<VocabularyItemBoundary> {
            if (ids.isEmpty()) return emptyList()
            vocabularySeeder.ensureSeeded()
            return wordDao.getByIds(ids).map { it.toBoundary() }
        }

        override suspend fun search(
            foldedQuery: String,
            levels: Set<String>,
            limit: Int,
        ): List<VocabularyItemBoundary> {
            vocabularySeeder.ensureSeeded()
            return wordDao
                .search(
                    foldedQuery = foldedQuery,
                    levels = levels.toList(),
                    ignoreLevels = if (levels.isEmpty()) 1 else 0,
                    limit = limit,
                ).map { it.toBoundary() }
        }

        override suspend fun syncFromSource(): SyncOutcomeBoundary = vocabularySeeder.sync()

        override suspend fun countWords(): Int = wordDao.count()

        override suspend fun countStudyWords(): Int {
            vocabularySeeder.ensureSeeded()
            return wordDao.countForStudy()
        }

        override suspend fun deleteWord(id: Long) {
            vocabularySeeder.ensureSeeded()
            wordDao.setDeleted(id, isDeleted = true)
        }

        override suspend fun restoreWord(id: Long) {
            vocabularySeeder.ensureSeeded()
            wordDao.setDeleted(id, isDeleted = false)
        }

        override suspend fun setFavourite(
            ids: List<Long>,
            isFavourite: Boolean,
        ) {
            if (ids.isEmpty()) return
            vocabularySeeder.ensureSeeded()
            wordDao.setFavourite(ids, isFavourite)
        }

        override fun observeFavouriteIds(): Flow<Set<Long>> = wordDao.observeFavouriteIds().map { it.toSet() }
    }

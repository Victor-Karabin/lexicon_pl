package com.lexicon.data.repository

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
        override suspend fun getRandomItems(count: Int): List<VocabularyItemBoundary> {
            vocabularySeeder.ensureSeeded()
            return wordDao.getRandomForStudy(count).map { it.toBoundary() }
        }

        override suspend fun getItemsByIds(ids: List<Long>): List<VocabularyItemBoundary> {
            if (ids.isEmpty()) return emptyList()
            vocabularySeeder.ensureSeeded()
            return wordDao.getByIds(ids).map { it.toBoundary() }
        }

        override suspend fun countStudyWords(): Int {
            vocabularySeeder.ensureSeeded()
            return wordDao.countForStudy()
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

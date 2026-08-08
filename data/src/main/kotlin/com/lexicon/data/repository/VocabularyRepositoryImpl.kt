package com.lexicon.data.repository

import com.lexicon.boundary.VocabularyItemBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.data.local.VocabularySeeder
import com.lexicon.data.local.WordDao
import com.lexicon.data.local.toBoundary
import javax.inject.Inject

class VocabularyRepositoryImpl
    @Inject
    constructor(
        private val wordDao: WordDao,
        private val vocabularySeeder: VocabularySeeder,
    ) : VocabularyRepository {
        override suspend fun getRandomItems(count: Int): List<VocabularyItemBoundary> {
            vocabularySeeder.ensureSeeded()
            return wordDao.getRandom(count).map { it.toBoundary() }
        }

        override suspend fun getItemsByIds(ids: List<Long>): List<VocabularyItemBoundary> {
            if (ids.isEmpty()) return emptyList()
            vocabularySeeder.ensureSeeded()
            return wordDao.getByIds(ids).map { it.toBoundary() }
        }
    }

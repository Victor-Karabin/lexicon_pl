package com.lexicon.pl.data.repository

import com.lexicon.pl.boundary.VocabularyItemBoundary
import com.lexicon.pl.boundary.VocabularyRepository
import com.lexicon.pl.data.local.WordDao
import com.lexicon.pl.data.local.WordEntity
import javax.inject.Inject

class VocabularyRepositoryImpl
    @Inject
    constructor(
        private val wordDao: WordDao,
    ) : VocabularyRepository {
        override suspend fun getRandomItems(count: Int): List<VocabularyItemBoundary> = wordDao.getRandom(count).map { it.toBoundary() }

        private fun WordEntity.toBoundary() =
            VocabularyItemBoundary(
                id = id,
                text = text,
                translation = translation,
                transcription = transcription,
            )
    }

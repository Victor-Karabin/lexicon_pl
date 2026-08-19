package com.lexicon.data.repository

import com.lexicon.boundary.SeedOutcomeBoundary
import com.lexicon.boundary.VocabularyItemBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.data.local.VocabularySeeder
import com.lexicon.data.local.WordDao
import com.lexicon.data.local.WordEntity
import com.lexicon.data.local.nextUserWordId
import com.lexicon.data.local.searchKeyFor
import com.lexicon.data.local.toBoundary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VocabularyRepositoryImpl(
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

    override suspend fun seedFromAsset(): SeedOutcomeBoundary = vocabularySeeder.sync()

    override suspend fun createWord(
        text: String,
        translation: String,
        transcription: String,
    ): VocabularyItemBoundary {
        vocabularySeeder.ensureSeeded()
        val word = WordEntity(
            id = nextUserWordId(wordDao.lowestId()),
            text = text,
            translation = translation,
            transcription = transcription,
            searchKey = searchKeyFor(text, translation),
            isUserCreated = true,
        )
        wordDao.insert(word)
        return word.toBoundary()
    }

    override suspend fun updateWord(
        id: Long,
        text: String,
        translation: String,
        transcription: String,
    ): VocabularyItemBoundary {
        vocabularySeeder.ensureSeeded()
        wordDao.updateWord(
            id = id,
            text = text,
            translation = translation,
            transcription = transcription,
            searchKey = searchKeyFor(text, translation),
        )
        return checkNotNull(wordDao.findById(id)) { "word $id vanished while being edited" }.toBoundary()
    }

    override suspend fun findWordByText(text: String): VocabularyItemBoundary? {
        vocabularySeeder.ensureSeeded()
        return wordDao.findByText(text)?.toBoundary()
    }

    override suspend fun allWordIds(): List<Long> {
        vocabularySeeder.ensureSeeded()
        return wordDao.allWordIds()
    }

    override suspend fun wordIdsForLevel(level: String): List<Long> {
        vocabularySeeder.ensureSeeded()
        return wordDao.wordIdsForLevel(level)
    }

    override suspend fun studySetWordIds(): List<Long> {
        vocabularySeeder.ensureSeeded()
        return wordDao.studySetWordIds()
    }

    override suspend fun getWord(id: Long): VocabularyItemBoundary? {
        vocabularySeeder.ensureSeeded()
        return wordDao.findById(id)?.toBoundary()
    }

    override suspend fun countWords(): Int = wordDao.count()

    override suspend fun countStudyWords(excludePhrases: Boolean): Int {
        vocabularySeeder.ensureSeeded()
        return wordDao.countForStudy(excludePhrases = if (excludePhrases) 1 else 0)
    }

    override suspend fun deleteWord(id: Long) {
        vocabularySeeder.ensureSeeded()
        wordDao.setDeleted(id, isDeleted = true)
    }

    override suspend fun restoreWord(id: Long) {
        vocabularySeeder.ensureSeeded()
        wordDao.setDeleted(id, isDeleted = false)
    }

    override suspend fun setInStudySet(
        ids: List<Long>,
        isInStudySet: Boolean,
    ) {
        if (ids.isEmpty()) return
        vocabularySeeder.ensureSeeded()
        wordDao.setInStudySet(ids, isInStudySet)
    }

    override fun observeStudySetIds(): Flow<Set<Long>> = wordDao.observeStudySetIds().map { it.toSet() }
}

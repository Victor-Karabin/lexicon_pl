package com.lexicon.data.repository

import com.lexicon.boundary.SeedOutcomeBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.data.local.MAX_SQL_VARIABLES
import com.lexicon.data.local.VocabularySeeder
import com.lexicon.data.local.WordDao
import com.lexicon.data.local.WordEntity
import com.lexicon.data.local.forEachBatch
import com.lexicon.data.local.inBatches
import com.lexicon.data.local.nextUserWordId
import com.lexicon.data.local.searchKeyFor
import com.lexicon.data.local.toWord
import com.lexicon.model.vocabulary.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VocabularyRepositoryImpl(
    private val wordDao: WordDao,
    private val vocabularySeeder: VocabularySeeder,
) : VocabularyRepository {
    override suspend fun getRandomItems(
        count: Int,
        restrictToIds: List<Long>,
    ): List<Word> {
        vocabularySeeder.ensureSeeded()
        val words = when {
            restrictToIds.isEmpty() -> wordDao.getRandomForStudy(count)
            restrictToIds.size <= MAX_SQL_VARIABLES -> wordDao.getRandomFromIds(restrictToIds, count)

            else -> restrictToIds.shuffled().inBatches { wordDao.getByIds(it) }.shuffled().take(count)
        }
        return words.map { it.toWord() }
    }

    override suspend fun getItemsByIds(ids: List<Long>): List<Word> {
        if (ids.isEmpty()) return emptyList()
        vocabularySeeder.ensureSeeded()
        return ids.inBatches { wordDao.getByIds(it) }.map { it.toWord() }
    }

    override suspend fun search(
        foldedQuery: String,
        levels: Set<String>,
        limit: Int,
    ): List<Word> {
        vocabularySeeder.ensureSeeded()
        return wordDao
            .search(
                foldedQuery = foldedQuery,
                levels = levels.toList(),
                ignoreLevels = if (levels.isEmpty()) 1 else 0,
                limit = limit,
            ).map { it.toWord() }
    }

    override suspend fun seedFromAsset(): SeedOutcomeBoundary = vocabularySeeder.sync()

    override suspend fun createWord(
        text: String,
        translation: String,
        transcription: String,
    ): Word {
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
        return word.toWord()
    }

    override suspend fun updateWord(
        id: Long,
        text: String,
        translation: String,
        transcription: String,
    ): Word {
        vocabularySeeder.ensureSeeded()
        wordDao.updateWord(
            id = id,
            text = text,
            translation = translation,
            transcription = transcription,
            searchKey = searchKeyFor(text, translation),
        )
        return checkNotNull(wordDao.findById(id)) { "word $id vanished while being edited" }.toWord()
    }

    override suspend fun findWordByText(text: String): Word? {
        vocabularySeeder.ensureSeeded()
        return wordDao.findByText(text)?.toWord()
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

    override suspend fun getWord(id: Long): Word? {
        vocabularySeeder.ensureSeeded()
        return wordDao.findById(id)?.toWord()
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
        ids.forEachBatch { wordDao.setInStudySet(it, isInStudySet) }
    }

    override fun observeStudySetIds(): Flow<Set<Long>> = wordDao.observeStudySetIds().map { it.toSet() }
}

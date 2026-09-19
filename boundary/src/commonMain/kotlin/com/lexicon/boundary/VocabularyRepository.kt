package com.lexicon.boundary

import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordStatus
import kotlinx.coroutines.flow.Flow

interface VocabularyRepository {
    suspend fun seedFromAsset(): SeedOutcomeBoundary

    suspend fun getRandomItems(
        count: Int,
        restrictToIds: List<Long> = emptyList(),
    ): List<Word>

    suspend fun getItemsByIds(ids: List<Long>): List<Word>

    suspend fun search(
        foldedQuery: String,
        levels: Set<String>,
        learningOnly: Boolean = false,
        limit: Int,
        offset: Int = 0,
    ): List<Word>

    suspend fun countStudyWords(excludePhrases: Boolean = false): Int

    suspend fun countWords(): Int

    suspend fun createWord(
        text: String,
        translation: String,
        transcription: String,
        example: String = "",
    ): Word

    suspend fun updateWord(
        id: Long,
        text: String,
        translation: String,
        transcription: String,
        example: String = "",
    ): Word

    suspend fun findWordByText(text: String): Word?

    suspend fun getWord(id: Long): Word?

    suspend fun deleteWord(id: Long)

    suspend fun restoreWord(id: Long)

    suspend fun setStatus(
        ids: List<Long>,
        status: WordStatus,
    )

    fun observeStudySetIds(): Flow<Set<Long>>

    fun observeWordStatuses(): Flow<Map<Long, WordStatus>>

    suspend fun wordsWithStatus(
        status: WordStatus,
        limit: Int,
    ): List<Word>

    suspend fun countWithStatus(status: WordStatus): Int

    suspend fun learningWordIds(limit: Int): List<Long>

    suspend fun randomKnownWordIds(limit: Int): List<Long>

    suspend fun allWordIds(): List<Long>

    suspend fun wordIdsForLevel(level: String): List<Long>

    suspend fun studySetWordIds(): List<Long>

    suspend fun studySetTextsAmong(texts: List<String>): Set<String>
}

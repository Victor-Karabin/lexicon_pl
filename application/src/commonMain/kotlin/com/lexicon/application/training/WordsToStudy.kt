package com.lexicon.application.training

import com.lexicon.application.settings.StepCountResolver
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.model.vocabulary.Word

suspend fun VocabularyRepository.wordsToStudy(vocabularyIds: List<Long>): List<Word> =
    getItemsByIds(vocabularyIds.ifEmpty { studySetWordIds() })

internal suspend fun VocabularyRepository.randomWords(
    resolver: StepCountResolver,
    requested: Int?,
    ids: List<Long>,
): List<Word> = getRandomItems(resolver.resolve(requested), ids)

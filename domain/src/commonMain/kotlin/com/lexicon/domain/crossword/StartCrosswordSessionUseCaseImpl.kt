@file:OptIn(ExperimentalUuidApi::class)

package com.lexicon.domain.crossword

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.dictation.isPhrase
import com.lexicon.domain.dictation.toWord
import com.lexicon.interactors.crossword.CrosswordSessionResponse
import com.lexicon.interactors.crossword.CrosswordWordPlacement
import com.lexicon.interactors.crossword.StartCrosswordSessionRequest
import com.lexicon.interactors.crossword.StartCrosswordSessionUseCase
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val CANDIDATE_POOL_SIZE = 200

class StartCrosswordSessionUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
) : StartCrosswordSessionUseCase {
    override suspend fun invoke(request: StartCrosswordSessionRequest): CrosswordSessionResponse {
        val words = vocabularyRepository.getRandomItems(CANDIDATE_POOL_SIZE, request.vocabularyIds)
            .map { it.toWord() }
            .filterNot { it.isPhrase }
            .take(request.wordCount)

        val layout = CrosswordGridGenerator.generate(words)

        val placements = layout.placements.map { placed ->
            CrosswordWordPlacement(
                vocabularyItemId = placed.word.id,
                expectedText = placed.word.text,
                clueText = placed.word.translation,
                row = placed.row,
                col = placed.col,
                direction = placed.direction,
            )
        }

        return CrosswordSessionResponse(
            sessionId = Uuid.random().toString(),
            words = placements,
            rowCount = layout.rowCount,
            colCount = layout.colCount,
        )
    }
}

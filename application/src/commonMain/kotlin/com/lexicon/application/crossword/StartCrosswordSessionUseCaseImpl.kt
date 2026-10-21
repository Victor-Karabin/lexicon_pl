package com.lexicon.application.crossword

import com.lexicon.application.training.open
import com.lexicon.boundary.SessionStore
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.crossword.CrosswordSessionResponse
import com.lexicon.interactors.crossword.CrosswordWordPlacement
import com.lexicon.interactors.crossword.StartCrosswordSessionRequest
import com.lexicon.interactors.crossword.StartCrosswordSessionUseCase
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId

private const val CANDIDATE_POOL_SIZE = 200

class StartCrosswordSessionUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val sessions: SessionStore,
) : StartCrosswordSessionUseCase {
    override suspend fun invoke(request: StartCrosswordSessionRequest): CrosswordSessionResponse {
        val words = vocabularyRepository.getRandomItems(CANDIDATE_POOL_SIZE, request.vocabularyIds)
            .filter(TrainingType.CROSSWORD::admits)
            .take(request.wordCount)

        val layout = CrosswordGridGenerator.generate(words)

        val placements = layout.placements.map { placed ->
            CrosswordWordPlacement(
                vocabularyItemId = placed.word.id.value,
                expectedText = placed.word.text,
                clueText = placed.word.translation,
                row = placed.row,
                col = placed.col,
                direction = placed.direction,
            )
        }

        val sessionId = sessions.open(TrainingType.CROSSWORD, placements.map { VocabularyId(it.vocabularyItemId) to it.expectedText })

        return CrosswordSessionResponse(
            sessionId = sessionId.value,
            words = placements,
            rowCount = layout.rowCount,
            colCount = layout.colCount,
        )
    }
}

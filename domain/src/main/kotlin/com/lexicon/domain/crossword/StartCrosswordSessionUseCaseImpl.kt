package com.lexicon.domain.crossword

import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.dictation.isPhrase
import com.lexicon.domain.dictation.toWord
import com.lexicon.interactors.crossword.CrosswordSessionResponse
import com.lexicon.interactors.crossword.CrosswordWordPlacement
import com.lexicon.interactors.crossword.StartCrosswordSessionRequest
import com.lexicon.interactors.crossword.StartCrosswordSessionUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.UUID
import javax.inject.Inject

/** Large enough to reliably find [StartCrosswordSessionRequest.wordCount] single-word (non-phrase) items. */
private const val CANDIDATE_POOL_SIZE = 200

class StartCrosswordSessionUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
        private val imageProvider: ImageProvider,
    ) : StartCrosswordSessionUseCase {
        override suspend fun invoke(request: StartCrosswordSessionRequest): CrosswordSessionResponse {
            // Crossword cells hold one letter each, so multi-word phrases can't be placed in the grid.
            val words = vocabularyRepository.getRandomItems(CANDIDATE_POOL_SIZE)
                .map { it.toWord() }
                .filterNot { it.isPhrase }
                .take(request.wordCount)

            val layout = CrosswordGridGenerator.generate(words)

            val placements = coroutineScope {
                layout.placements.map { placed -> async { buildPlacement(placed) } }.awaitAll()
            }

            return CrosswordSessionResponse(
                sessionId = UUID.randomUUID().toString(),
                words = placements,
                rowCount = layout.rowCount,
                colCount = layout.colCount,
            )
        }

        private suspend fun buildPlacement(placed: PlacedWord): CrosswordWordPlacement =
            CrosswordWordPlacement(
                vocabularyItemId = placed.word.id,
                expectedText = placed.word.text,
                imageUrl = imageProvider.searchImage(placed.word.translation),
                clueText = placed.word.translation,
                row = placed.row,
                col = placed.col,
                direction = placed.direction,
            )
    }

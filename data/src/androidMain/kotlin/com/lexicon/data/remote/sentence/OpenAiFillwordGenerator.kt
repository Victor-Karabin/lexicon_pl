package com.lexicon.data.remote.sentence

import com.lexicon.boundary.FillwordGenerator
import com.lexicon.boundary.FillwordPlacementBoundary
import com.lexicon.boundary.FillwordRequestBoundary
import com.lexicon.boundary.FillwordResultBoundary
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val PROMPT = """# Role

You generate Fillword (Word Search) puzzles for an application for learning Polish.

## Input

words: {{words}}
gridSize: {{gridSize}}
difficulty: {{difficulty}}

## Task

Generate a valid Fillword puzzle by placing all provided Polish words into a square grid of letters.

## Placement rules

* Words may be placed horizontally, vertically, or diagonally.
* Words may be placed forwards or backwards.
* Words must remain completely inside the grid.
* Words may cross or share letters.
* Every target word must occur in the generated grid exactly as placed.
* Fill all remaining cells with Polish letters.
* Use Polish alphabet characters when appropriate: Ą Ć Ę Ł Ń Ó Ś Ź Ż
* Do not replace Polish characters with their Latin equivalents.
* Do not use spaces, hyphens, or punctuation inside words.
* Do not modify the supplied words.
* Each supplied word must be findable in the final grid.

## Difficulty

EASY: prefer horizontal and vertical placement, fewer diagonals, avoid backwards words.
MEDIUM: horizontal, vertical and diagonal, forwards and backwards, moderate overlap.
HARD: all directions, prefer diagonal and backwards, significant overlap, unused letters
less distinguishable from target-word sequences.

## Output

Return only valid JSON:

{"grid":[["A","B","C"],["D","E","F"],["G","H","I"]],
 "words":[{"word":"DOM","start":{"row":0,"column":0},"end":{"row":0,"column":2},"direction":"RIGHT"}]}

Direction values, only these:
UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT

## Validation

Before returning, verify that the grid is exactly gridSize by gridSize, every input word
exists in the grid, every start and end coordinate is correct, every direction is correct,
no word extends outside the grid, the characters along each reported path spell the word,
and the JSON is syntactically valid.

Do not return explanations, Markdown, comments, or additional text.
"""

@Serializable
private data class FillwordJson(
    val grid: List<List<String>> = emptyList(),
    val words: List<FillwordWordJson> = emptyList(),
)

@Serializable
private data class FillwordWordJson(
    val word: String = "",
    val start: FillwordCellJson = FillwordCellJson(),
    val direction: String = "",
)

@Serializable
private data class FillwordCellJson(
    val row: Int = 0,
    val column: Int = 0,
)

class OpenAiFillwordGenerator(
    private val api: OpenAiApi,
) : FillwordGenerator {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generate(request: FillwordRequestBoundary): FillwordResultBoundary {
        val filled = PROMPT
            .replace("{{words}}", request.words.joinToString(", "))
            .replace("{{gridSize}}", request.gridSize.toString())
            .replace("{{difficulty}}", request.difficulty)

        return when (val answer = api.ask(filled)) {
            OpenAiAnswer.Offline -> FillwordResultBoundary.Offline
            is OpenAiAnswer.Failed -> FillwordResultBoundary.Refused(answer.reason)
            is OpenAiAnswer.Text ->
                runCatching { json.decodeFromString(FillwordJson.serializer(), answer.text.unfenced()) }
                    .map { it.toBoundary() }
                    .getOrElse { FillwordResultBoundary.Refused("unreadable puzzle") }
        }
    }
}

private fun FillwordJson.toBoundary(): FillwordResultBoundary =
    if (grid.isEmpty()) {
        FillwordResultBoundary.Refused("empty grid")
    } else {
        FillwordResultBoundary.Generated(
            grid = grid,
            placements = words.map {
                FillwordPlacementBoundary(
                    word = it.word,
                    startRow = it.start.row,
                    startColumn = it.start.column,
                    direction = it.direction,
                )
            },
        )
    }

private fun String.unfenced(): String =
    trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()

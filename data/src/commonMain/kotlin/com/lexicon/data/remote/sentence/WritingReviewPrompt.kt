package com.lexicon.data.remote.sentence

import com.lexicon.boundary.WritingReviewRequestBoundary
import com.lexicon.boundary.WritingReviewResultBoundary
import com.lexicon.model.course.WritingReview
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val PROMPT = """# Role

You are a friendly Polish teacher checking homework from a complete beginner (A1)
who has just finished one lesson. Your feedback is in English.

## Task the learner was given

{{task}}

## The learner's answers

{{answers}}

## What the lesson taught

Judge the answers against this, and only this. Do not expect grammar or vocabulary
the lesson has not taught yet.

{{criteria}}

## A model answer, for reference

The learner's own wording is welcome; use this only to see what is possible.

{{model}}

## Output

Return only JSON, with no Markdown around it:

{"strengths": ["..."], "improvements": ["..."]}

* strengths: one to three short sentences on what the learner did well.
* improvements: up to four short sentences, most important first. Quote the
  learner's Polish exactly and give the corrected Polish. Leave the list empty
  when there is nothing to fix.
* Point out only real mistakes: wrong words, wrong forms, missing Polish
  letters, or formal and informal mixed up. Never swap a correct phrase for
  another correct one, such as "Miło mi" for "Bardzo mi miło".
* If an answer is empty, not in Polish, or misses the task, say so as an
  improvement.
"""

@Serializable
private data class ReviewJson(
    val strengths: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
)

private val reviewJson = Json { ignoreUnknownKeys = true }

fun writingReviewPrompt(request: WritingReviewRequestBoundary): String =
    PROMPT
        .replace("{{task}}", request.task)
        .replace("{{answers}}", request.answers.joinToString("\n\n") { "### ${it.label}\n${it.text.ifBlank { "(empty)" }}" })
        .replace("{{criteria}}", request.criteria.joinToString("\n") { "* $it" })
        .replace("{{model}}", request.model.joinToString("\n\n"))

fun parseWritingReview(text: String): WritingReview? {
    val start = text.indexOf('{')
    val end = text.lastIndexOf('}')
    if (start < 0 || end <= start) return null
    val parsed = runCatching { reviewJson.decodeFromString<ReviewJson>(text.substring(start, end + 1)) }.getOrNull() ?: return null
    val strengths = parsed.strengths.map { it.trim() }.filter { it.isNotEmpty() }
    val improvements = parsed.improvements.map { it.trim() }.filter { it.isNotEmpty() }
    if (strengths.isEmpty() && improvements.isEmpty()) return null
    return WritingReview(strengths.toImmutableList(), improvements.toImmutableList())
}

fun OpenAiAnswer.toReviewResult(): WritingReviewResultBoundary =
    when (this) {
        is OpenAiAnswer.Text ->
            parseWritingReview(text)?.let(WritingReviewResultBoundary::Reviewed)
                ?: WritingReviewResultBoundary.Refused("the reply was not a review")

        OpenAiAnswer.Offline -> WritingReviewResultBoundary.Offline
        is OpenAiAnswer.Failed -> WritingReviewResultBoundary.Refused(reason)
    }

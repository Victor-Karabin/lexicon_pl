package com.lexicon.domain.course

import com.lexicon.boundary.CourseBoundary
import com.lexicon.boundary.LessonAudioBoundary
import com.lexicon.boundary.LessonBoundary
import com.lexicon.boundary.LessonExerciseBoundary
import com.lexicon.boundary.LessonSummaryBoundary
import com.lexicon.interactors.course.Course
import com.lexicon.interactors.course.CourseId
import com.lexicon.interactors.course.GapFillItem
import com.lexicon.interactors.course.LETTER_GAP
import com.lexicon.interactors.course.Lesson
import com.lexicon.interactors.course.LessonAudio
import com.lexicon.interactors.course.LessonExercise
import com.lexicon.interactors.course.LessonId
import com.lexicon.interactors.course.LessonSummary
import com.lexicon.interactors.course.LetterFillItem
import com.lexicon.interactors.course.MatchItem
import com.lexicon.interactors.course.MinimalPairItem
import com.lexicon.interactors.course.TranscribeItem
import com.lexicon.model.vocabulary.LocalizedText
import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.toImmutableList

fun CourseBoundary.toCourse(): Course =
    Course(
        id = CourseId(id),
        order = order,
        level = level,
        title = LocalizedText(title),
        lessons = lessons
            .mapIndexed { index, lesson -> lesson.toSummary(LessonUnlockRule.isUnlocked(lessons, index)) }
            .toImmutableList(),
    )

fun LessonSummaryBoundary.toSummary(isUnlocked: Boolean): LessonSummary =
    LessonSummary(
        id = LessonId(id),
        number = number,
        title = title,
        wordCount = wordCount,
        isCompleted = isCompleted,
        isUnlocked = isUnlocked,
    )

fun LessonBoundary.toLesson(): Lesson =
    Lesson(
        id = LessonId(id),
        courseId = CourseId(courseId),
        number = number,
        title = title,
        vocabularyIds = vocabularyIds.map(::VocabularyId).toImmutableList(),
        audio = audio.map(LessonAudioBoundary::toAudio).toImmutableList(),
        exercises = exercises.mapNotNull(LessonExerciseBoundary::toExercise).toImmutableList(),
        isCompleted = isCompleted,
    )

fun LessonAudioBoundary.toAudio(): LessonAudio =
    LessonAudio(
        file = file,
        section = section,
        task = task,
        part = part,
        remoteId = remoteId,
    )

private const val TYPE_REPEAT = "repeat"
private const val TYPE_MINIMAL_PAIR = "minimal_pair"
private const val TYPE_GAP_FILL = "gap_fill"
private const val TYPE_TRANSCRIBE = "transcribe"
private const val TYPE_MATCH = "match"
private const val TYPE_LETTER_FILL = "letter_fill"

fun LessonExerciseBoundary.toExercise(): LessonExercise? =
    when (type) {
        TYPE_REPEAT ->
            LessonExercise.Repeat(
                id = id,
                instruction = instruction,
                audioFile = audioFile,
                words = items.mapNotNull { it.prompt }.toImmutableList(),
            )

        TYPE_MINIMAL_PAIR ->
            LessonExercise.MinimalPair(
                id = id,
                instruction = instruction,
                audioFile = audioFile,
                items = items
                    .filter { it.options.size == 2 && it.answers.isNotEmpty() }
                    .map {
                        MinimalPairItem(
                            label = it.label.orEmpty(),
                            options = it.options.toImmutableList(),
                            answer = it.answers.first(),
                        )
                    }.toImmutableList(),
            )

        TYPE_GAP_FILL ->
            LessonExercise.GapFill(
                id = id,
                instruction = instruction,
                audioFile = audioFile,
                items = items
                    .filter { it.prompt != null }
                    .map {
                        GapFillItem(
                            prompt = it.prompt.orEmpty(),
                            answers = it.answers.toImmutableList(),
                            speaker = it.label?.takeIf(String::isNotBlank),
                        )
                    }.toImmutableList(),
            )

        TYPE_TRANSCRIBE ->
            LessonExercise.Transcribe(
                id = id,
                instruction = instruction,
                audioFile = audioFile,
                items = items
                    .filter { it.answers.isNotEmpty() }
                    .map { TranscribeItem(label = it.label.orEmpty(), answer = it.answers.first()) }
                    .toImmutableList(),
            )

        TYPE_MATCH ->
            LessonExercise.Match(
                id = id,
                instruction = instruction,
                audioFile = audioFile,
                items = items
                    .filter { it.prompt != null && it.answers.isNotEmpty() }
                    .map {
                        MatchItem(
                            label = it.label.orEmpty(),
                            prompt = it.prompt.orEmpty(),
                            answer = it.answers.first(),
                        )
                    }.toImmutableList(),
            )

        TYPE_LETTER_FILL ->
            LessonExercise.LetterFill(
                id = id,
                instruction = instruction,
                audioFile = audioFile,
                items = items
                    .filter { it.prompt != null && it.answers.isNotEmpty() }
                    .filter { it.prompt.orEmpty().count { gap -> gap == LETTER_GAP } == it.answers.size }
                    .map {
                        LetterFillItem(
                            label = it.label.orEmpty(),
                            pattern = it.prompt.orEmpty(),
                            letters = it.answers.toImmutableList(),
                        )
                    }.toImmutableList(),
            )

        else -> null
    }

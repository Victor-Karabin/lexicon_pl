package com.lexicon.domain.course

import com.lexicon.boundary.CourseBoundary
import com.lexicon.boundary.LessonAudioBoundary
import com.lexicon.boundary.LessonBoundary
import com.lexicon.boundary.LessonSectionBoundary
import com.lexicon.boundary.LessonSummaryBoundary
import com.lexicon.interactors.course.Course
import com.lexicon.interactors.course.CourseId
import com.lexicon.interactors.course.Lesson
import com.lexicon.interactors.course.LessonAudio
import com.lexicon.interactors.course.LessonAudioSource
import com.lexicon.interactors.course.LessonId
import com.lexicon.interactors.course.LessonSection
import com.lexicon.interactors.course.LessonSummary
import com.lexicon.interactors.presets.LocalizedText
import com.lexicon.interactors.presets.VocabularyId
import kotlinx.collections.immutable.toImmutableList

private const val WORKBOOK_SOURCE = "workbook"

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
        sections = sections.map(LessonSectionBoundary::toSection).toImmutableList(),
        vocabularyIds = vocabularyIds.map(::VocabularyId).toImmutableList(),
        audio = audio.map(LessonAudioBoundary::toAudio).toImmutableList(),
        isCompleted = isCompleted,
    )

fun LessonSectionBoundary.toSection(): LessonSection = LessonSection(letter = letter, title = title)

fun LessonAudioBoundary.toAudio(): LessonAudio =
    LessonAudio(
        file = file,
        source = if (source == WORKBOOK_SOURCE) LessonAudioSource.WORKBOOK else LessonAudioSource.COURSEBOOK,
        section = section,
        task = task,
        part = part,
    )

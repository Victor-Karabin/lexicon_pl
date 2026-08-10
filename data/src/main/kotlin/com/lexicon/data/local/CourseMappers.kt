package com.lexicon.data.local

import com.lexicon.boundary.LessonAudioBoundary
import com.lexicon.boundary.LessonBoundary
import com.lexicon.boundary.LessonSectionBoundary

/** Where a track came from, so the lesson screen can label the workbook's separately. */
object LessonAudioSource {
    const val COURSEBOOK = "coursebook"
    const val WORKBOOK = "workbook"
}

fun CourseAsset.toEntity(): CourseEntity = CourseEntity(id = id, sortOrder = order, level = level, titleJson = title.encodeLocalized())

fun LessonAsset.toEntity(): LessonEntity =
    LessonEntity(
        id = id,
        courseId = courseId,
        number = number,
        title = title,
        communicationJson = communication.encodeList(),
        vocabularyTopicsJson = vocabularyTopics.encodeList(),
        grammarJson = grammar.encodeList(),
    )

fun LessonAsset.toSectionEntities(): List<LessonSectionEntity> =
    sections.mapIndexed { index, section ->
        LessonSectionEntity(lessonId = id, letter = section.letter, title = section.title, position = index)
    }

fun LessonAsset.toWordEntities(): List<LessonWordEntity> =
    vocabularyIds
        .distinct()
        .mapIndexed { index, wordId -> LessonWordEntity(lessonId = id, wordId = wordId, position = index) }

fun LessonAsset.toAudioEntities(): List<LessonAudioEntity> =
    (audio.map { it to LessonAudioSource.COURSEBOOK } + workbookAudio.map { it to LessonAudioSource.WORKBOOK })
        .mapIndexed { index, (track, source) ->
            LessonAudioEntity(
                lessonId = id,
                file = track.file,
                source = source,
                section = track.section,
                task = track.task,
                part = track.part,
                position = index,
            )
        }

fun LessonEntity.toBoundary(
    sections: List<LessonSectionEntity>,
    wordIds: List<Long>,
    audio: List<LessonAudioEntity>,
    isCompleted: Boolean,
): LessonBoundary =
    LessonBoundary(
        id = id,
        courseId = courseId,
        number = number,
        title = title,
        communication = communicationJson.decodeList(),
        vocabularyTopics = vocabularyTopicsJson.decodeList(),
        grammar = grammarJson.decodeList(),
        sections = sections.map { LessonSectionBoundary(letter = it.letter, title = it.title) },
        vocabularyIds = wordIds,
        audio = audio.map {
            LessonAudioBoundary(
                file = it.file,
                source = it.source,
                section = it.section,
                task = it.task,
                part = it.part,
            )
        },
        isCompleted = isCompleted,
    )

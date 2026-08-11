package com.lexicon.data.local

import com.lexicon.boundary.LessonAudioBoundary
import com.lexicon.boundary.LessonBoundary

fun CourseAsset.toEntity(): CourseEntity = CourseEntity(id = id, sortOrder = order, level = level, titleJson = title.encodeLocalized())

fun LessonAsset.toEntity(): LessonEntity =
    LessonEntity(
        id = id,
        courseId = courseId,
        number = number,
        title = title,
    )

fun LessonAsset.toWordEntities(): List<LessonWordEntity> =
    vocabularyIds
        .distinct()
        .mapIndexed { index, wordId -> LessonWordEntity(lessonId = id, wordId = wordId, position = index) }

fun LessonAsset.toAudioEntities(): List<LessonAudioEntity> =
    audio
        .mapIndexed { index, track ->
            LessonAudioEntity(
                lessonId = id,
                file = track.file,
                section = track.section,
                task = track.task,
                part = track.part,
                position = index,
                remoteId = track.remoteId,
            )
        }

fun LessonEntity.toBoundary(
    wordIds: List<Long>,
    audio: List<LessonAudioEntity>,
    isCompleted: Boolean,
): LessonBoundary =
    LessonBoundary(
        id = id,
        courseId = courseId,
        number = number,
        title = title,
        vocabularyIds = wordIds,
        audio = audio.map {
            LessonAudioBoundary(
                file = it.file,
                section = it.section,
                task = it.task,
                part = it.part,
                remoteId = it.remoteId,
            )
        },
        isCompleted = isCompleted,
    )

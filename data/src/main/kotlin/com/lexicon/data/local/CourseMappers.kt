package com.lexicon.data.local

import com.lexicon.boundary.ExerciseItemBoundary
import com.lexicon.boundary.LessonAudioBoundary
import com.lexicon.boundary.LessonBoundary
import com.lexicon.boundary.LessonExerciseBoundary

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
    exercises: List<LessonExerciseBoundary>,
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
        exercises = exercises,
        isCompleted = isCompleted,
    )

fun LessonAsset.toExerciseEntities(): List<LessonExerciseEntity> =
    exercises.mapIndexed { index, exercise ->
        LessonExerciseEntity(
            id = exercise.id,
            lessonId = id,
            tag = exercise.tag,
            type = exercise.type,
            instruction = exercise.instruction,
            audioFile = exercise.audioFile,
            position = index,
        )
    }

fun LessonAsset.toExerciseItemEntities(): List<LessonExerciseItemEntity> =
    exercises.flatMap { exercise ->
        exercise.items.mapIndexed { index, item ->
            LessonExerciseItemEntity(
                exerciseId = exercise.id,
                position = index,
                label = item.label,
                // A repeat item carries only text; it reads as the prompt.
                prompt = item.prompt ?: item.text,
                optionsJson = item.options.encodeList(),
                answersJson = (item.answers + listOfNotNull(item.answer)).encodeList(),
            )
        }
    }

fun LessonExerciseEntity.toBoundary(items: List<LessonExerciseItemEntity>): LessonExerciseBoundary =
    LessonExerciseBoundary(
        id = id,
        type = type,
        instruction = instruction,
        audioFile = audioFile,
        items = items.sortedBy { it.position }.map {
            ExerciseItemBoundary(
                label = it.label,
                prompt = it.prompt,
                options = it.optionsJson.decodeList(),
                answers = it.answersJson.decodeList(),
            )
        },
    )

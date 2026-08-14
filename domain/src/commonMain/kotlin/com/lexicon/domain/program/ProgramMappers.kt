package com.lexicon.domain.program

import com.lexicon.boundary.ProgramBoundary
import com.lexicon.boundary.ProgramEnrolmentBoundary
import com.lexicon.interactors.presets.LocalizedText
import com.lexicon.interactors.program.EnrolmentStatus
import com.lexicon.interactors.program.Program
import com.lexicon.interactors.program.ProgramConfig
import com.lexicon.interactors.program.ProgramDifficulty
import com.lexicon.interactors.program.ProgramEnrolment
import com.lexicon.interactors.program.ProgramId
import com.lexicon.interactors.program.ProgramVisibility
import kotlinx.serialization.json.Json

private val programJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Parses a program's configuration.
 *
 * This layer is the only one that knows what the configuration means: it arrives
 * from the data layer as an opaque string precisely so that storing a program and
 * understanding one stay separate concerns.
 *
 * A configuration that will not parse falls back to an empty one rather than
 * bringing the screen down. An empty program does nothing, which is visible and
 * recoverable; a crash on a malformed asset is neither.
 */
internal fun String.toProgramConfig(): ProgramConfig =
    runCatching { programJson.decodeFromString<ProgramConfig>(this) }.getOrElse { ProgramConfig() }

fun ProgramBoundary.toProgram(): Program =
    Program(
        id = ProgramId(id),
        level = level,
        order = order,
        title = LocalizedText(title),
        description = LocalizedText(description),
        difficulty = difficulty.toDifficulty(),
        estimatedDays = estimatedDays,
        visibility = visibility.toVisibility(),
        config = configJson.toProgramConfig(),
    )

fun ProgramEnrolmentBoundary.toEnrolment(): ProgramEnrolment =
    ProgramEnrolment(
        programId = ProgramId(programId),
        startedAtEpochDay = startedAtEpochDay,
        status = status.toStatus(),
        completedAtEpochDay = completedAtEpochDay,
    )

fun ProgramEnrolment.toBoundary(): ProgramEnrolmentBoundary =
    ProgramEnrolmentBoundary(
        programId = programId.value,
        startedAtEpochDay = startedAtEpochDay,
        status = status.name,
        completedAtEpochDay = completedAtEpochDay,
    )

// An unrecognised name means the asset and this build disagree; the mildest reading
// keeps the program usable rather than failing the whole catalogue over a label.
private fun String.toDifficulty(): ProgramDifficulty =
    ProgramDifficulty.entries.firstOrNull { it.name == uppercase() } ?: ProgramDifficulty.BEGINNER

private fun String.toVisibility(): ProgramVisibility =
    ProgramVisibility.entries.firstOrNull { it.name == uppercase() } ?: ProgramVisibility.PUBLIC

private fun String.toStatus(): EnrolmentStatus = EnrolmentStatus.entries.firstOrNull { it.name == uppercase() } ?: EnrolmentStatus.ACTIVE

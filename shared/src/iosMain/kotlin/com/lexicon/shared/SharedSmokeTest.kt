package com.lexicon.shared

import com.lexicon.data.local.AppDatabase
import com.lexicon.interactors.course.CheckExerciseAnswerUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Proves the shared code actually runs on iOS, rather than merely linking.
 *
 * Two things are worth checking end to end, because they are the two halves of
 * what this migration moved: a use case resolved from Koin and executed (shared
 * business logic), and a real query against a Room database opened through the
 * bundled SQLite driver on an iOS filesystem path (shared persistence).
 */
object SharedSmokeTest : KoinComponent {
    /** Runs both checks and returns a line per result for the host to display. */
    suspend fun run(): String {
        val logic = checkAnswerNormalisation()
        val database = countWordRows()
        return "$logic\n$database"
    }

    private fun checkAnswerNormalisation(): String {
        val checkAnswer: CheckExerciseAnswerUseCase = get()
        // Same trimming/case-folding the trainings rely on.
        val matches = checkAnswer("kot", "  KOT  ")
        return if (matches) "OK   use case: 'kot' matches '  KOT  '" else "FAIL use case: normalisation did not match"
    }

    private suspend fun countWordRows(): String {
        val database: AppDatabase = get()
        // A fresh install has no rows; what matters is that Room opened the file,
        // created the schema and answered a query without throwing.
        val count = database.wordDao().count()
        return "OK   room: opened lexicon.db and counted $count word rows"
    }
}

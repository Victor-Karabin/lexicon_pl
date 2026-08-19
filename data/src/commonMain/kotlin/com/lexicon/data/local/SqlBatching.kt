package com.lexicon.data.local

/**
 * SQLite refuses a statement with more host parameters than it allows, and on Android 8
 * to 11 that limit is 999 — comfortably fewer than the words in a preset like Top 1000,
 * or in a program scoped to the whole catalogue. Anything that passes a list of ids to a
 * query has to be cut into batches this size.
 */
internal const val MAX_SQL_VARIABLES = 900

internal suspend fun <T> List<Long>.inBatches(query: suspend (List<Long>) -> List<T>): List<T> =
    if (size <= MAX_SQL_VARIABLES) query(this) else chunked(MAX_SQL_VARIABLES).flatMap { query(it) }

internal suspend fun List<Long>.forEachBatch(statement: suspend (List<Long>) -> Unit) {
    if (size <= MAX_SQL_VARIABLES) statement(this) else chunked(MAX_SQL_VARIABLES).forEach { statement(it) }
}

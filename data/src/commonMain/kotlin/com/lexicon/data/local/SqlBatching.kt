package com.lexicon.data.local

internal const val MAX_SQL_VARIABLES = 900

internal suspend fun <T> List<Long>.inBatches(query: suspend (List<Long>) -> List<T>): List<T> =
    if (size <= MAX_SQL_VARIABLES) query(this) else chunked(MAX_SQL_VARIABLES).flatMap { query(it) }

internal suspend fun List<Long>.forEachBatch(statement: suspend (List<Long>) -> Unit) {
    if (size <= MAX_SQL_VARIABLES) statement(this) else chunked(MAX_SQL_VARIABLES).forEach { statement(it) }
}

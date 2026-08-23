package com.lexicon.data.local

internal const val MAX_SQL_VARIABLES = 900

internal suspend fun <A, T> List<A>.inBatches(query: suspend (List<A>) -> List<T>): List<T> =
    if (size <= MAX_SQL_VARIABLES) query(this) else chunked(MAX_SQL_VARIABLES).flatMap { query(it) }

internal suspend fun <A> List<A>.forEachBatch(statement: suspend (List<A>) -> Unit) {
    if (size <= MAX_SQL_VARIABLES) statement(this) else chunked(MAX_SQL_VARIABLES).forEach { statement(it) }
}

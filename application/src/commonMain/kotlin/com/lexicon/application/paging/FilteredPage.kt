package com.lexicon.application.paging

private const val LIST_PAGE = 40

private const val PAGES_BEFORE_GIVING_UP = 20

internal data class FilteredPage<T>(
    val items: List<T>,
    val nextOffset: Int,
    val isLast: Boolean,
)

internal suspend fun <R, T : Any> filteredPage(
    offset: Int,
    fetch: suspend (limit: Int, offset: Int) -> List<R>,
    accept: (R) -> T?,
): FilteredPage<T> {
    val found = mutableListOf<T>()

    var cursor = offset
    var reads = 0
    var isLast = false
    while (found.size < LIST_PAGE && reads < PAGES_BEFORE_GIVING_UP) {
        val batchStart = cursor
        val batch = fetch(LIST_PAGE, batchStart)
        reads++

        for (row in batch) {
            cursor++
            accept(row)?.let { found += it }
            if (found.size == LIST_PAGE) break
        }

        if (batch.size < LIST_PAGE && cursor == batchStart + batch.size) {
            isLast = true
            break
        }
    }
    return FilteredPage(items = found, nextOffset = cursor, isLast = isLast)
}

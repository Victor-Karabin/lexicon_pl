package com.lexicon.pl.common

import kotlinx.coroutines.CoroutineDispatcher

/** Injectable source of coroutine dispatchers so call sites never reference [kotlinx.coroutines.Dispatchers] directly. */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

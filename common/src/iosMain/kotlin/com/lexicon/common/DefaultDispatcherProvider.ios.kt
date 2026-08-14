package com.lexicon.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Kotlin/Native has no public Dispatchers.IO at this coroutines version. Default is
 * the right stand-in here rather than a hand-rolled pool: on Native it is already a
 * multi-threaded dispatcher, not the CPU-bound-only one it is on the JVM.
 */
internal actual val platformIoDispatcher: CoroutineDispatcher get() = Dispatchers.Default

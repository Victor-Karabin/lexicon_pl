package com.lexicon.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Dispatchers.IO is not in coroutines' common source set, so each platform names its own. */
internal expect val platformIoDispatcher: CoroutineDispatcher

class DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher get() = Dispatchers.Main
    override val io: CoroutineDispatcher get() = platformIoDispatcher
    override val default: CoroutineDispatcher get() = Dispatchers.Default
}

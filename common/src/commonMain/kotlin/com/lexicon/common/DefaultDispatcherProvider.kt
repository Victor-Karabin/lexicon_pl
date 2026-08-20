package com.lexicon.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal expect val platformIoDispatcher: CoroutineDispatcher

class DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher get() = Dispatchers.Main
    override val io: CoroutineDispatcher get() = platformIoDispatcher
    override val default: CoroutineDispatcher get() = Dispatchers.Default
}

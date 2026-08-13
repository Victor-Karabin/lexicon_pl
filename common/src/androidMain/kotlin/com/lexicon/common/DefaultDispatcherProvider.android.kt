package com.lexicon.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val platformIoDispatcher: CoroutineDispatcher get() = Dispatchers.IO

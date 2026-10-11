package com.lexicon.presentation.common

import android.util.Log
import com.lexicon.common.DispatcherProvider
import com.lexicon.common.runSuspendCatching
import com.lexicon.interactors.training.RecordStudyActivityUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "StudyActivity"

fun RecordStudyActivityUseCase.markIn(
    scope: CoroutineScope,
    dispatchers: DispatcherProvider,
) {
    scope.launch(dispatchers.io) {
        runSuspendCatching { invoke() }.onFailure { Log.w(TAG, "Study time could not be recorded", it) }
    }
}

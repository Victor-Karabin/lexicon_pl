package com.lexicon.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.training.CheckTrainingReadinessUseCase
import com.lexicon.interactors.training.TrainingReadiness
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrainingGateViewModel
    @Inject
    constructor(
        private val checkReadiness: CheckTrainingReadinessUseCase,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel() {
        private val _readiness = MutableStateFlow<TrainingReadiness?>(null)
        val readiness: StateFlow<TrainingReadiness?> = _readiness.asStateFlow()

        fun check(
            minimumWords: Int,
            excludePhrases: Boolean = false,
        ) {
            viewModelScope.launch(dispatchers.io) {
                _readiness.value = checkReadiness(minimumWords, excludePhrases)
            }
        }
    }

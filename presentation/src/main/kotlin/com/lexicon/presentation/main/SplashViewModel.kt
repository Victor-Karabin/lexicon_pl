package com.lexicon.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.sync.CatalogSyncStatus
import com.lexicon.interactors.sync.SyncCatalogUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val syncCatalog: SyncCatalogUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val _status = MutableStateFlow(CatalogSyncStatus())
    val status: StateFlow<CatalogSyncStatus> = _status.asStateFlow()

    init {
        start()
    }

    fun start() {
        _status.value = CatalogSyncStatus()
        viewModelScope.launch(dispatchers.io) {
            syncCatalog().collect { _status.value = it }
        }
    }
}

package com.lexicon.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.sync.CatalogSeedStatus
import com.lexicon.interactors.sync.SeedCatalogsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val seedCatalogs: SeedCatalogsUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val _status = MutableStateFlow(CatalogSeedStatus())
    val status: StateFlow<CatalogSeedStatus> = _status.asStateFlow()

    init {
        start()
    }

    fun start() {
        _status.value = CatalogSeedStatus()
        viewModelScope.launch(dispatchers.io) {
            seedCatalogs().collect { _status.value = it }
        }
    }
}

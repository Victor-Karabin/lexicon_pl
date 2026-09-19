package com.lexicon.presentation.vocabularycourse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.interactors.vocabularycourse.CourseSettings
import com.lexicon.interactors.vocabularycourse.GetVocabularyCourseUseCase
import com.lexicon.interactors.vocabularycourse.UpdateCourseSettingsUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val MIN_NEW_WORDS_A_DAY = 1

data class CourseSettingsUiState(
    val isLoading: Boolean = true,
    val newWordsADay: Int = CourseSettings.DEFAULT_NEW_WORDS_A_DAY,
    val reviewsADay: Int = CourseSettings.DEFAULT_REVIEWS_A_DAY,
    val queue: ImmutableList<String> = persistentListOf(),
    val keptLastTraining: Boolean = false,
    val isClosing: Boolean = false,
)

class CourseSettingsViewModel(
    private val getCourse: GetVocabularyCourseUseCase,
    private val updateSettings: UpdateCourseSettingsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CourseSettingsUiState())
    val uiState: StateFlow<CourseSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = getCourse().settings
            _uiState.update {
                it.copy(
                    isLoading = false,
                    newWordsADay = settings.newWordsADay,
                    reviewsADay = settings.reviewsADay,
                    queue = settings.queue.toImmutableList(),
                )
            }
        }
    }

    fun onNewWordsChanged(value: Int) = _uiState.update { it.copy(newWordsADay = value) }

    fun onReviewsChanged(value: Int) = _uiState.update { it.copy(reviewsADay = value) }

    fun onTrainingAdded(id: String) =
        _uiState.update { state ->
            state.copy(queue = (state.queue + id).toImmutableList(), keptLastTraining = false)
        }

    fun onTurnRemoved(index: Int) =
        _uiState.update { state ->
            when {
                index !in state.queue.indices -> state
                state.queue.size == 1 -> state.copy(keptLastTraining = true)
                else -> state.copy(queue = state.queue.toMutableList().apply { removeAt(index) }.toImmutableList())
            }
        }

    fun onMove(
        from: Int,
        to: Int,
    ) = _uiState.update { state ->
        if (from !in state.queue.indices || to !in state.queue.indices) return@update state
        val queue = state.queue.toMutableList()
        queue.add(to, queue.removeAt(from))
        state.copy(queue = queue.toImmutableList())
    }

    fun onDone(close: () -> Unit) {
        val state = _uiState.value
        if (state.isClosing) return
        if (state.isLoading) {
            close()
            return
        }
        _uiState.update { it.copy(isClosing = true) }

        viewModelScope.launch {
            updateSettings(
                CourseSettings(
                    newWordsADay = state.newWordsADay,
                    reviewsADay = state.reviewsADay,
                    queue = state.queue,
                ),
            )
            close()
        }
    }
}

package com.lexicon.presentation.presets

import com.lexicon.interactors.presets.GetWordPresetMembershipsUseCase
import com.lexicon.interactors.presets.PresetMembership
import com.lexicon.interactors.presets.SetWordPresetMembershipUseCase
import com.lexicon.model.vocabulary.PresetId
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

data class ChangePresetsUiState(
    val wordId: VocabularyId,
    val word: String,
    val languageTag: String,
    val memberships: ImmutableList<PresetMembership> = persistentListOf(),
    val isLoading: Boolean = true,
) {
    val memberCount: Int get() = memberships.count { it.isMember }
}

class ChangePresetsController(
    private val scope: CoroutineScope,
    private val ioContext: CoroutineContext,
    private val getMemberships: GetWordPresetMembershipsUseCase,
    private val setMembership: SetWordPresetMembershipUseCase,
    private val onChanged: suspend () -> Unit = {},
) {
    private val _state = MutableStateFlow<ChangePresetsUiState?>(null)
    val state: StateFlow<ChangePresetsUiState?> = _state.asStateFlow()

    fun open(
        word: Word,
        languageTag: String,
    ) {
        _state.value = ChangePresetsUiState(wordId = word.id, word = word.text, languageTag = languageTag)
        scope.launch(ioContext) {
            val memberships = getMemberships(word.id)
            _state.update { current ->

                current?.takeIf { it.wordId == word.id }?.copy(memberships = memberships, isLoading = false)
            }
        }
    }

    fun dismiss() {
        _state.value = null
    }

    fun toggle(
        presetId: PresetId,
        isMember: Boolean,
    ) {
        val wordId = _state.value?.wordId ?: return
        _state.update { current ->
            current?.copy(
                memberships = current.memberships
                    .map { if (it.preset.id == presetId) it.copy(isMember = isMember) else it }
                    .toImmutableList(),
            )
        }
        scope.launch(ioContext) {
            setMembership(presetId, wordId, isMember)
            onChanged()
        }
    }
}

package com.lexicon.presentation.presets

import com.lexicon.interactors.presets.GetWordPresetMembershipsUseCase
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetMembership
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.SetWordPresetMembershipUseCase
import com.lexicon.interactors.presets.VocabularyId
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

/**
 * The change-presets sheet's state and behaviour, kept out of the ViewModels so the
 * Vocabulary tab and a preset's own word list drive it identically.
 *
 * A toggle is applied to local state straight away and persisted behind it: the chip
 * has to respond to the tap, and there is nothing to roll back to — the sheet shows
 * the truth again next time it opens.
 */
class ChangePresetsController(
    private val scope: CoroutineScope,
    private val ioContext: CoroutineContext,
    private val getMemberships: GetWordPresetMembershipsUseCase,
    private val setMembership: SetWordPresetMembershipUseCase,
) {
    private val _state = MutableStateFlow<ChangePresetsUiState?>(null)
    val state: StateFlow<ChangePresetsUiState?> = _state.asStateFlow()

    fun open(
        word: PresetWord,
        languageTag: String,
    ) {
        _state.value = ChangePresetsUiState(wordId = word.id, word = word.text, languageTag = languageTag)
        scope.launch(ioContext) {
            val memberships = getMemberships(word.id)
            _state.update { current ->
                // The sheet may have been dismissed while this was loading.
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
        scope.launch(ioContext) { setMembership(presetId, wordId, isMember) }
    }
}

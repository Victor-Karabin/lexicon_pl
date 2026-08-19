package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.PresetCategory
import kotlinx.collections.immutable.ImmutableList

interface GetPresetCategoriesUseCase {
    suspend operator fun invoke(): ImmutableList<PresetCategory>
}

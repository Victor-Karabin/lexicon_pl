package com.lexicon.interactors.presets

import kotlinx.collections.immutable.ImmutableList

interface GetPresetCategoriesUseCase {
    suspend operator fun invoke(): ImmutableList<PresetCategory>
}

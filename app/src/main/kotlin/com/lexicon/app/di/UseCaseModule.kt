package com.lexicon.app.di

import com.lexicon.domain.crossword.StartCrosswordSessionUseCaseImpl
import com.lexicon.domain.crossword.SubmitCrosswordUseCaseImpl
import com.lexicon.domain.dictation.StartDictationSessionUseCaseImpl
import com.lexicon.domain.dictation.SubmitDictationAnswerUseCaseImpl
import com.lexicon.domain.dictationpuzzle.StartDictationPuzzleSessionUseCaseImpl
import com.lexicon.domain.dictationpuzzle.SubmitDictationPuzzleAnswerUseCaseImpl
import com.lexicon.domain.imagetest.StartImageTestSessionUseCaseImpl
import com.lexicon.domain.imagetest.SubmitImageTestAnswerUseCaseImpl
import com.lexicon.domain.memorycards.StartMemoryCardsSessionUseCaseImpl
import com.lexicon.domain.memorycards.SubmitMemoryCardsStepResultUseCaseImpl
import com.lexicon.domain.mix.StartMixSessionUseCaseImpl
import com.lexicon.domain.presets.DeletePresetUseCaseImpl
import com.lexicon.domain.presets.DeleteWordUseCaseImpl
import com.lexicon.domain.presets.GetPresetCategoriesUseCaseImpl
import com.lexicon.domain.presets.GetPresetVocabularyUseCaseImpl
import com.lexicon.domain.presets.GetVocabularyPresetUseCaseImpl
import com.lexicon.domain.presets.GetVocabularyPresetsUseCaseImpl
import com.lexicon.domain.presets.ObserveFavouriteWordIdsUseCaseImpl
import com.lexicon.domain.presets.ObserveVocabularyPresetsUseCaseImpl
import com.lexicon.domain.presets.RestorePresetUseCaseImpl
import com.lexicon.domain.presets.RestoreWordUseCaseImpl
import com.lexicon.domain.presets.SearchVocabularyUseCaseImpl
import com.lexicon.domain.presets.SetPresetFavouriteUseCaseImpl
import com.lexicon.domain.presets.ToggleWordFavouriteUseCaseImpl
import com.lexicon.domain.pronunciation.StartPronunciationSessionUseCaseImpl
import com.lexicon.domain.pronunciation.SubmitPronunciationResultUseCaseImpl
import com.lexicon.domain.puzzle.StartPuzzleSessionUseCaseImpl
import com.lexicon.domain.puzzle.SubmitPuzzleAnswerUseCaseImpl
import com.lexicon.domain.settings.ObserveSettingsUseCaseImpl
import com.lexicon.domain.settings.UpdateStepCountUseCaseImpl
import com.lexicon.domain.settings.UpdateThemeModeUseCaseImpl
import com.lexicon.domain.sync.SyncCatalogUseCaseImpl
import com.lexicon.domain.training.CheckTrainingReadinessUseCaseImpl
import com.lexicon.domain.trueorfalse.StartTrueOrFalseSessionUseCaseImpl
import com.lexicon.domain.trueorfalse.SubmitTrueOrFalseAnswerUseCaseImpl
import com.lexicon.domain.wordmatch.StartWordMatchSessionUseCaseImpl
import com.lexicon.domain.wordmatch.SubmitWordMatchStepResultUseCaseImpl
import com.lexicon.interactors.crossword.StartCrosswordSessionUseCase
import com.lexicon.interactors.crossword.SubmitCrosswordUseCase
import com.lexicon.interactors.dictation.StartDictationSessionUseCase
import com.lexicon.interactors.dictation.SubmitDictationAnswerUseCase
import com.lexicon.interactors.dictationpuzzle.StartDictationPuzzleSessionUseCase
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerUseCase
import com.lexicon.interactors.imagetest.StartImageTestSessionUseCase
import com.lexicon.interactors.imagetest.SubmitImageTestAnswerUseCase
import com.lexicon.interactors.memorycards.StartMemoryCardsSessionUseCase
import com.lexicon.interactors.memorycards.SubmitMemoryCardsStepResultUseCase
import com.lexicon.interactors.mix.StartMixSessionUseCase
import com.lexicon.interactors.presets.DeletePresetUseCase
import com.lexicon.interactors.presets.DeleteWordUseCase
import com.lexicon.interactors.presets.GetPresetCategoriesUseCase
import com.lexicon.interactors.presets.GetPresetVocabularyUseCase
import com.lexicon.interactors.presets.GetVocabularyPresetUseCase
import com.lexicon.interactors.presets.GetVocabularyPresetsUseCase
import com.lexicon.interactors.presets.ObserveFavouriteWordIdsUseCase
import com.lexicon.interactors.presets.ObserveVocabularyPresetsUseCase
import com.lexicon.interactors.presets.RestorePresetUseCase
import com.lexicon.interactors.presets.RestoreWordUseCase
import com.lexicon.interactors.presets.SearchVocabularyUseCase
import com.lexicon.interactors.presets.SetPresetFavouriteUseCase
import com.lexicon.interactors.presets.ToggleWordFavouriteUseCase
import com.lexicon.interactors.pronunciation.StartPronunciationSessionUseCase
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultUseCase
import com.lexicon.interactors.puzzle.StartPuzzleSessionUseCase
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerUseCase
import com.lexicon.interactors.settings.ObserveSettingsUseCase
import com.lexicon.interactors.settings.UpdateStepCountUseCase
import com.lexicon.interactors.settings.UpdateThemeModeUseCase
import com.lexicon.interactors.sync.SyncCatalogUseCase
import com.lexicon.interactors.training.CheckTrainingReadinessUseCase
import com.lexicon.interactors.trueorfalse.StartTrueOrFalseSessionUseCase
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerUseCase
import com.lexicon.interactors.wordmatch.StartWordMatchSessionUseCase
import com.lexicon.interactors.wordmatch.SubmitWordMatchStepResultUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {
    @Binds
    abstract fun bindStartDictationSessionUseCase(impl: StartDictationSessionUseCaseImpl): StartDictationSessionUseCase

    @Binds
    abstract fun bindSubmitDictationAnswerUseCase(impl: SubmitDictationAnswerUseCaseImpl): SubmitDictationAnswerUseCase

    @Binds
    abstract fun bindStartDictationPuzzleSessionUseCase(impl: StartDictationPuzzleSessionUseCaseImpl): StartDictationPuzzleSessionUseCase

    @Binds
    abstract fun bindSubmitDictationPuzzleAnswerUseCase(impl: SubmitDictationPuzzleAnswerUseCaseImpl): SubmitDictationPuzzleAnswerUseCase

    @Binds
    abstract fun bindStartTrueOrFalseSessionUseCase(impl: StartTrueOrFalseSessionUseCaseImpl): StartTrueOrFalseSessionUseCase

    @Binds
    abstract fun bindSubmitTrueOrFalseAnswerUseCase(impl: SubmitTrueOrFalseAnswerUseCaseImpl): SubmitTrueOrFalseAnswerUseCase

    @Binds
    abstract fun bindStartWordMatchSessionUseCase(impl: StartWordMatchSessionUseCaseImpl): StartWordMatchSessionUseCase

    @Binds
    abstract fun bindSubmitWordMatchStepResultUseCase(impl: SubmitWordMatchStepResultUseCaseImpl): SubmitWordMatchStepResultUseCase

    @Binds
    abstract fun bindStartPronunciationSessionUseCase(impl: StartPronunciationSessionUseCaseImpl): StartPronunciationSessionUseCase

    @Binds
    abstract fun bindSubmitPronunciationResultUseCase(impl: SubmitPronunciationResultUseCaseImpl): SubmitPronunciationResultUseCase

    @Binds
    abstract fun bindStartPuzzleSessionUseCase(impl: StartPuzzleSessionUseCaseImpl): StartPuzzleSessionUseCase

    @Binds
    abstract fun bindSubmitPuzzleAnswerUseCase(impl: SubmitPuzzleAnswerUseCaseImpl): SubmitPuzzleAnswerUseCase

    @Binds
    abstract fun bindStartImageTestSessionUseCase(impl: StartImageTestSessionUseCaseImpl): StartImageTestSessionUseCase

    @Binds
    abstract fun bindSubmitImageTestAnswerUseCase(impl: SubmitImageTestAnswerUseCaseImpl): SubmitImageTestAnswerUseCase

    @Binds
    abstract fun bindStartMemoryCardsSessionUseCase(impl: StartMemoryCardsSessionUseCaseImpl): StartMemoryCardsSessionUseCase

    @Binds
    abstract fun bindSubmitMemoryCardsStepResultUseCase(impl: SubmitMemoryCardsStepResultUseCaseImpl): SubmitMemoryCardsStepResultUseCase

    @Binds
    abstract fun bindStartCrosswordSessionUseCase(impl: StartCrosswordSessionUseCaseImpl): StartCrosswordSessionUseCase

    @Binds
    abstract fun bindObserveSettingsUseCase(impl: ObserveSettingsUseCaseImpl): ObserveSettingsUseCase

    @Binds
    abstract fun bindUpdateThemeModeUseCase(impl: UpdateThemeModeUseCaseImpl): UpdateThemeModeUseCase

    @Binds
    abstract fun bindUpdateStepCountUseCase(impl: UpdateStepCountUseCaseImpl): UpdateStepCountUseCase

    @Binds
    abstract fun bindSubmitCrosswordUseCase(impl: SubmitCrosswordUseCaseImpl): SubmitCrosswordUseCase

    @Binds
    abstract fun bindStartMixSessionUseCase(impl: StartMixSessionUseCaseImpl): StartMixSessionUseCase

    @Binds
    abstract fun bindGetVocabularyPresets(impl: GetVocabularyPresetsUseCaseImpl): GetVocabularyPresetsUseCase

    @Binds
    abstract fun bindGetPresetCategories(impl: GetPresetCategoriesUseCaseImpl): GetPresetCategoriesUseCase

    @Binds
    abstract fun bindGetVocabularyPreset(impl: GetVocabularyPresetUseCaseImpl): GetVocabularyPresetUseCase

    @Binds
    abstract fun bindGetPresetVocabulary(impl: GetPresetVocabularyUseCaseImpl): GetPresetVocabularyUseCase

    @Binds
    abstract fun bindToggleWordFavourite(impl: ToggleWordFavouriteUseCaseImpl): ToggleWordFavouriteUseCase

    @Binds
    abstract fun bindSetPresetFavourite(impl: SetPresetFavouriteUseCaseImpl): SetPresetFavouriteUseCase

    @Binds
    abstract fun bindObserveFavouriteWordIds(impl: ObserveFavouriteWordIdsUseCaseImpl): ObserveFavouriteWordIdsUseCase

    @Binds
    abstract fun bindCheckTrainingReadiness(impl: CheckTrainingReadinessUseCaseImpl): CheckTrainingReadinessUseCase

    @Binds
    abstract fun bindSearchVocabulary(impl: SearchVocabularyUseCaseImpl): SearchVocabularyUseCase

    @Binds
    abstract fun bindSyncCatalog(impl: SyncCatalogUseCaseImpl): SyncCatalogUseCase

    @Binds
    abstract fun bindDeleteWord(impl: DeleteWordUseCaseImpl): DeleteWordUseCase

    @Binds
    abstract fun bindRestoreWord(impl: RestoreWordUseCaseImpl): RestoreWordUseCase

    @Binds
    abstract fun bindDeletePreset(impl: DeletePresetUseCaseImpl): DeletePresetUseCase

    @Binds
    abstract fun bindRestorePreset(impl: RestorePresetUseCaseImpl): RestorePresetUseCase

    @Binds
    abstract fun bindObserveVocabularyPresets(impl: ObserveVocabularyPresetsUseCaseImpl): ObserveVocabularyPresetsUseCase
}

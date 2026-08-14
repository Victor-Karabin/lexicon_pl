package com.lexicon.domain.di

import com.lexicon.domain.course.CheckExerciseAnswerUseCaseImpl
import com.lexicon.domain.course.GetLessonUseCaseImpl
import com.lexicon.domain.course.GetLessonVocabularyUseCaseImpl
import com.lexicon.domain.course.ObserveCoursesUseCaseImpl
import com.lexicon.domain.course.SetLessonCompletedUseCaseImpl
import com.lexicon.domain.crossword.StartCrosswordSessionUseCaseImpl
import com.lexicon.domain.crossword.SubmitCrosswordUseCaseImpl
import com.lexicon.domain.dictation.AnswerNormalizer
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
import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.domain.settings.UpdateStepCountUseCaseImpl
import com.lexicon.domain.settings.UpdateThemeModeUseCaseImpl
import com.lexicon.domain.sync.SyncCatalogUseCaseImpl
import com.lexicon.domain.training.CheckTrainingReadinessUseCaseImpl
import com.lexicon.domain.trueorfalse.StartTrueOrFalseSessionUseCaseImpl
import com.lexicon.domain.trueorfalse.SubmitTrueOrFalseAnswerUseCaseImpl
import com.lexicon.domain.wordmatch.StartWordMatchSessionUseCaseImpl
import com.lexicon.domain.wordmatch.SubmitWordMatchStepResultUseCaseImpl
import com.lexicon.interactors.course.CheckExerciseAnswerUseCase
import com.lexicon.interactors.course.GetLessonUseCase
import com.lexicon.interactors.course.GetLessonVocabularyUseCase
import com.lexicon.interactors.course.ObserveCoursesUseCase
import com.lexicon.interactors.course.SetLessonCompletedUseCase
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
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val domainModule = module {
    factoryOf(::AnswerNormalizer)
    factoryOf(::StepCountResolver)

    factoryOf(::StartDictationSessionUseCaseImpl) { bind<StartDictationSessionUseCase>() }
    factoryOf(::SubmitDictationAnswerUseCaseImpl) { bind<SubmitDictationAnswerUseCase>() }
    factoryOf(::StartDictationPuzzleSessionUseCaseImpl) { bind<StartDictationPuzzleSessionUseCase>() }
    factoryOf(::SubmitDictationPuzzleAnswerUseCaseImpl) { bind<SubmitDictationPuzzleAnswerUseCase>() }
    factoryOf(::StartTrueOrFalseSessionUseCaseImpl) { bind<StartTrueOrFalseSessionUseCase>() }
    factoryOf(::SubmitTrueOrFalseAnswerUseCaseImpl) { bind<SubmitTrueOrFalseAnswerUseCase>() }
    factoryOf(::StartWordMatchSessionUseCaseImpl) { bind<StartWordMatchSessionUseCase>() }
    factoryOf(::SubmitWordMatchStepResultUseCaseImpl) { bind<SubmitWordMatchStepResultUseCase>() }
    factoryOf(::StartPronunciationSessionUseCaseImpl) { bind<StartPronunciationSessionUseCase>() }
    factoryOf(::SubmitPronunciationResultUseCaseImpl) { bind<SubmitPronunciationResultUseCase>() }
    factoryOf(::StartPuzzleSessionUseCaseImpl) { bind<StartPuzzleSessionUseCase>() }
    factoryOf(::SubmitPuzzleAnswerUseCaseImpl) { bind<SubmitPuzzleAnswerUseCase>() }
    factoryOf(::StartImageTestSessionUseCaseImpl) { bind<StartImageTestSessionUseCase>() }
    factoryOf(::SubmitImageTestAnswerUseCaseImpl) { bind<SubmitImageTestAnswerUseCase>() }
    factoryOf(::StartMemoryCardsSessionUseCaseImpl) { bind<StartMemoryCardsSessionUseCase>() }
    factoryOf(::SubmitMemoryCardsStepResultUseCaseImpl) { bind<SubmitMemoryCardsStepResultUseCase>() }
    factoryOf(::StartCrosswordSessionUseCaseImpl) { bind<StartCrosswordSessionUseCase>() }
    factoryOf(::SubmitCrosswordUseCaseImpl) { bind<SubmitCrosswordUseCase>() }
    factoryOf(::ObserveSettingsUseCaseImpl) { bind<ObserveSettingsUseCase>() }
    factoryOf(::UpdateThemeModeUseCaseImpl) { bind<UpdateThemeModeUseCase>() }
    factoryOf(::UpdateStepCountUseCaseImpl) { bind<UpdateStepCountUseCase>() }
    factoryOf(::StartMixSessionUseCaseImpl) { bind<StartMixSessionUseCase>() }
    factoryOf(::GetVocabularyPresetsUseCaseImpl) { bind<GetVocabularyPresetsUseCase>() }
    factoryOf(::GetPresetCategoriesUseCaseImpl) { bind<GetPresetCategoriesUseCase>() }
    factoryOf(::GetVocabularyPresetUseCaseImpl) { bind<GetVocabularyPresetUseCase>() }
    factoryOf(::GetPresetVocabularyUseCaseImpl) { bind<GetPresetVocabularyUseCase>() }
    factoryOf(::ToggleWordFavouriteUseCaseImpl) { bind<ToggleWordFavouriteUseCase>() }
    factoryOf(::SetPresetFavouriteUseCaseImpl) { bind<SetPresetFavouriteUseCase>() }
    factoryOf(::ObserveFavouriteWordIdsUseCaseImpl) { bind<ObserveFavouriteWordIdsUseCase>() }
    factoryOf(::CheckTrainingReadinessUseCaseImpl) { bind<CheckTrainingReadinessUseCase>() }
    factoryOf(::SearchVocabularyUseCaseImpl) { bind<SearchVocabularyUseCase>() }
    factoryOf(::SyncCatalogUseCaseImpl) { bind<SyncCatalogUseCase>() }
    factoryOf(::DeleteWordUseCaseImpl) { bind<DeleteWordUseCase>() }
    factoryOf(::RestoreWordUseCaseImpl) { bind<RestoreWordUseCase>() }
    factoryOf(::DeletePresetUseCaseImpl) { bind<DeletePresetUseCase>() }
    factoryOf(::RestorePresetUseCaseImpl) { bind<RestorePresetUseCase>() }
    factoryOf(::ObserveVocabularyPresetsUseCaseImpl) { bind<ObserveVocabularyPresetsUseCase>() }
    factoryOf(::ObserveCoursesUseCaseImpl) { bind<ObserveCoursesUseCase>() }
    factoryOf(::GetLessonUseCaseImpl) { bind<GetLessonUseCase>() }
    factoryOf(::GetLessonVocabularyUseCaseImpl) { bind<GetLessonVocabularyUseCase>() }
    factoryOf(::SetLessonCompletedUseCaseImpl) { bind<SetLessonCompletedUseCase>() }
    factoryOf(::CheckExerciseAnswerUseCaseImpl) { bind<CheckExerciseAnswerUseCase>() }
}

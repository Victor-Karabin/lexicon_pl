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
import com.lexicon.domain.fillword.StartFillwordSessionUseCaseImpl
import com.lexicon.domain.imagetest.StartImageTestSessionUseCaseImpl
import com.lexicon.domain.imagetest.SubmitImageTestAnswerUseCaseImpl
import com.lexicon.domain.memorycards.StartMemoryCardsSessionUseCaseImpl
import com.lexicon.domain.memorycards.SubmitMemoryCardsStepResultUseCaseImpl
import com.lexicon.domain.mix.StartMixSessionUseCaseImpl
import com.lexicon.domain.passage.StartPassageSessionUseCaseImpl
import com.lexicon.domain.passage.SubmitPassageAnswersUseCaseImpl
import com.lexicon.domain.presets.CreatePresetUseCaseImpl
import com.lexicon.domain.presets.CreateWordUseCaseImpl
import com.lexicon.domain.presets.DeletePresetUseCaseImpl
import com.lexicon.domain.presets.DeleteWordUseCaseImpl
import com.lexicon.domain.presets.GetPinnedImageUseCaseImpl
import com.lexicon.domain.presets.GetPresetCategoriesUseCaseImpl
import com.lexicon.domain.presets.GetPresetVocabularyUseCaseImpl
import com.lexicon.domain.presets.GetVocabularyPresetUseCaseImpl
import com.lexicon.domain.presets.GetVocabularyPresetsUseCaseImpl
import com.lexicon.domain.presets.GetWordPresetMembershipsUseCaseImpl
import com.lexicon.domain.presets.GetWordUseCaseImpl
import com.lexicon.domain.presets.ObserveFavouriteWordIdsUseCaseImpl
import com.lexicon.domain.presets.ObserveVocabularyPresetsUseCaseImpl
import com.lexicon.domain.presets.RestorePresetUseCaseImpl
import com.lexicon.domain.presets.RestoreWordUseCaseImpl
import com.lexicon.domain.presets.SearchImageCandidatesUseCaseImpl
import com.lexicon.domain.presets.SearchVocabularyUseCaseImpl
import com.lexicon.domain.presets.SetPresetFavouriteUseCaseImpl
import com.lexicon.domain.presets.SetWordPresetMembershipUseCaseImpl
import com.lexicon.domain.presets.ToggleWordFavouriteUseCaseImpl
import com.lexicon.domain.presets.TranslateWordUseCaseImpl
import com.lexicon.domain.presets.UpdateWordUseCaseImpl
import com.lexicon.domain.program.AdvanceProgramDayUseCaseImpl
import com.lexicon.domain.program.CountFavouritesUseCaseImpl
import com.lexicon.domain.program.CreateProgramUseCaseImpl
import com.lexicon.domain.program.EnrolInProgramUseCaseImpl
import com.lexicon.domain.program.GetProgramDayUseCaseImpl
import com.lexicon.domain.program.GetProgramProgressUseCaseImpl
import com.lexicon.domain.program.GetProgramUseCaseImpl
import com.lexicon.domain.program.GetStudyStreakUseCaseImpl
import com.lexicon.domain.program.GetWordCardsUseCaseImpl
import com.lexicon.domain.program.LeaveProgramUseCaseImpl
import com.lexicon.domain.program.MarkCardsSeenUseCaseImpl
import com.lexicon.domain.program.ObserveActiveEnrolmentUseCaseImpl
import com.lexicon.domain.program.ObserveProgramsUseCaseImpl
import com.lexicon.domain.program.ResolveProgramScopeUseCaseImpl
import com.lexicon.domain.program.StartProgramSessionUseCaseImpl
import com.lexicon.domain.program.UpdateProgramUseCaseImpl
import com.lexicon.domain.pronunciation.StartPronunciationSessionUseCaseImpl
import com.lexicon.domain.pronunciation.SubmitPronunciationResultUseCaseImpl
import com.lexicon.domain.puzzle.StartPuzzleSessionUseCaseImpl
import com.lexicon.domain.puzzle.SubmitPuzzleAnswerUseCaseImpl
import com.lexicon.domain.settings.ObserveSettingsUseCaseImpl
import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.domain.settings.UpdateStepCountUseCaseImpl
import com.lexicon.domain.settings.UpdateThemeModeUseCaseImpl
import com.lexicon.domain.settings.UpdateVoiceUseCaseImpl
import com.lexicon.domain.sync.SyncCatalogUseCaseImpl
import com.lexicon.domain.training.CheckTrainingReadinessUseCaseImpl
import com.lexicon.domain.trueorfalse.StartTrueOrFalseSessionUseCaseImpl
import com.lexicon.domain.trueorfalse.SubmitTrueOrFalseAnswerUseCaseImpl
import com.lexicon.domain.wordcard.RecordWordCardSeenUseCaseImpl
import com.lexicon.domain.wordcard.StartWordCardSessionUseCaseImpl
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
import com.lexicon.interactors.fillword.StartFillwordSessionUseCase
import com.lexicon.interactors.imagetest.StartImageTestSessionUseCase
import com.lexicon.interactors.imagetest.SubmitImageTestAnswerUseCase
import com.lexicon.interactors.memorycards.StartMemoryCardsSessionUseCase
import com.lexicon.interactors.memorycards.SubmitMemoryCardsStepResultUseCase
import com.lexicon.interactors.mix.StartMixSessionUseCase
import com.lexicon.interactors.passage.StartPassageSessionUseCase
import com.lexicon.interactors.passage.SubmitPassageAnswersUseCase
import com.lexicon.interactors.presets.CreatePresetUseCase
import com.lexicon.interactors.presets.CreateWordUseCase
import com.lexicon.interactors.presets.DeletePresetUseCase
import com.lexicon.interactors.presets.DeleteWordUseCase
import com.lexicon.interactors.presets.GetPinnedImageUseCase
import com.lexicon.interactors.presets.GetPresetCategoriesUseCase
import com.lexicon.interactors.presets.GetPresetVocabularyUseCase
import com.lexicon.interactors.presets.GetVocabularyPresetUseCase
import com.lexicon.interactors.presets.GetVocabularyPresetsUseCase
import com.lexicon.interactors.presets.GetWordPresetMembershipsUseCase
import com.lexicon.interactors.presets.GetWordUseCase
import com.lexicon.interactors.presets.ObserveFavouriteWordIdsUseCase
import com.lexicon.interactors.presets.ObserveVocabularyPresetsUseCase
import com.lexicon.interactors.presets.RestorePresetUseCase
import com.lexicon.interactors.presets.RestoreWordUseCase
import com.lexicon.interactors.presets.SearchImageCandidatesUseCase
import com.lexicon.interactors.presets.SearchVocabularyUseCase
import com.lexicon.interactors.presets.SetPresetFavouriteUseCase
import com.lexicon.interactors.presets.SetWordPresetMembershipUseCase
import com.lexicon.interactors.presets.ToggleWordFavouriteUseCase
import com.lexicon.interactors.presets.TranslateWordUseCase
import com.lexicon.interactors.presets.UpdateWordUseCase
import com.lexicon.interactors.program.AdvanceProgramDayUseCase
import com.lexicon.interactors.program.CountFavouritesUseCase
import com.lexicon.interactors.program.CreateProgramUseCase
import com.lexicon.interactors.program.EnrolInProgramUseCase
import com.lexicon.interactors.program.GetProgramDayUseCase
import com.lexicon.interactors.program.GetProgramProgressUseCase
import com.lexicon.interactors.program.GetProgramUseCase
import com.lexicon.interactors.program.GetStudyStreakUseCase
import com.lexicon.interactors.program.GetWordCardsUseCase
import com.lexicon.interactors.program.LeaveProgramUseCase
import com.lexicon.interactors.program.MarkCardsSeenUseCase
import com.lexicon.interactors.program.ObserveActiveEnrolmentUseCase
import com.lexicon.interactors.program.ObserveProgramsUseCase
import com.lexicon.interactors.program.ResolveProgramScopeUseCase
import com.lexicon.interactors.program.StartProgramSessionUseCase
import com.lexicon.interactors.program.UpdateProgramUseCase
import com.lexicon.interactors.pronunciation.StartPronunciationSessionUseCase
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultUseCase
import com.lexicon.interactors.puzzle.StartPuzzleSessionUseCase
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerUseCase
import com.lexicon.interactors.settings.ObserveSettingsUseCase
import com.lexicon.interactors.settings.UpdateStepCountUseCase
import com.lexicon.interactors.settings.UpdateThemeModeUseCase
import com.lexicon.interactors.settings.UpdateVoiceUseCase
import com.lexicon.interactors.sync.SyncCatalogUseCase
import com.lexicon.interactors.training.CheckTrainingReadinessUseCase
import com.lexicon.interactors.trueorfalse.StartTrueOrFalseSessionUseCase
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerUseCase
import com.lexicon.interactors.wordcard.RecordWordCardSeenUseCase
import com.lexicon.interactors.wordcard.StartWordCardSessionUseCase
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
    factoryOf(::UpdateVoiceUseCaseImpl) { bind<UpdateVoiceUseCase>() }
    factoryOf(::StartMixSessionUseCaseImpl) { bind<StartMixSessionUseCase>() }
    factoryOf(::GetVocabularyPresetsUseCaseImpl) { bind<GetVocabularyPresetsUseCase>() }
    factoryOf(::GetPresetCategoriesUseCaseImpl) { bind<GetPresetCategoriesUseCase>() }
    factoryOf(::GetVocabularyPresetUseCaseImpl) { bind<GetVocabularyPresetUseCase>() }
    factoryOf(::GetPresetVocabularyUseCaseImpl) { bind<GetPresetVocabularyUseCase>() }
    factoryOf(::ToggleWordFavouriteUseCaseImpl) { bind<ToggleWordFavouriteUseCase>() }
    factoryOf(::SetPresetFavouriteUseCaseImpl) { bind<SetPresetFavouriteUseCase>() }
    factoryOf(::GetWordPresetMembershipsUseCaseImpl) { bind<GetWordPresetMembershipsUseCase>() }
    factoryOf(::SetWordPresetMembershipUseCaseImpl) { bind<SetWordPresetMembershipUseCase>() }
    factoryOf(::ObserveProgramsUseCaseImpl) { bind<ObserveProgramsUseCase>() }
    factoryOf(::GetProgramUseCaseImpl) { bind<GetProgramUseCase>() }
    factoryOf(::ObserveActiveEnrolmentUseCaseImpl) { bind<ObserveActiveEnrolmentUseCase>() }
    factoryOf(::EnrolInProgramUseCaseImpl) { bind<EnrolInProgramUseCase>() }
    factoryOf(::LeaveProgramUseCaseImpl) { bind<LeaveProgramUseCase>() }
    factoryOf(::ResolveProgramScopeUseCaseImpl) { bind<ResolveProgramScopeUseCase>() }
    factoryOf(::StartProgramSessionUseCaseImpl) { bind<StartProgramSessionUseCase>() }
    factoryOf(::GetProgramProgressUseCaseImpl) { bind<GetProgramProgressUseCase>() }
    factoryOf(::GetStudyStreakUseCaseImpl) { bind<GetStudyStreakUseCase>() }
    factoryOf(::CreateProgramUseCaseImpl) { bind<CreateProgramUseCase>() }
    factoryOf(::UpdateProgramUseCaseImpl) { bind<UpdateProgramUseCase>() }
    factoryOf(::StartWordCardSessionUseCaseImpl) { bind<StartWordCardSessionUseCase>() }
    factoryOf(::RecordWordCardSeenUseCaseImpl) { bind<RecordWordCardSeenUseCase>() }
    factoryOf(::CountFavouritesUseCaseImpl) { bind<CountFavouritesUseCase>() }
    factoryOf(::GetProgramDayUseCaseImpl) { bind<GetProgramDayUseCase>() }
    factoryOf(::AdvanceProgramDayUseCaseImpl) { bind<AdvanceProgramDayUseCase>() }
    factoryOf(::StartPassageSessionUseCaseImpl) { bind<StartPassageSessionUseCase>() }
    factoryOf(::StartFillwordSessionUseCaseImpl) { bind<StartFillwordSessionUseCase>() }
    factoryOf(::SubmitPassageAnswersUseCaseImpl) { bind<SubmitPassageAnswersUseCase>() }
    factoryOf(::MarkCardsSeenUseCaseImpl) { bind<MarkCardsSeenUseCase>() }
    factoryOf(::GetWordCardsUseCaseImpl) { bind<GetWordCardsUseCase>() }
    factoryOf(::CreateWordUseCaseImpl) { bind<CreateWordUseCase>() }
    factoryOf(::UpdateWordUseCaseImpl) { bind<UpdateWordUseCase>() }
    factoryOf(::GetWordUseCaseImpl) { bind<GetWordUseCase>() }
    factoryOf(::CreatePresetUseCaseImpl) { bind<CreatePresetUseCase>() }
    factoryOf(::TranslateWordUseCaseImpl) { bind<TranslateWordUseCase>() }
    factoryOf(::SearchImageCandidatesUseCaseImpl) { bind<SearchImageCandidatesUseCase>() }
    factoryOf(::GetPinnedImageUseCaseImpl) { bind<GetPinnedImageUseCase>() }
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

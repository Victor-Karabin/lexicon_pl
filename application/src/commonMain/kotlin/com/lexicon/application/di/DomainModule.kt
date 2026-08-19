package com.lexicon.application.di

import com.lexicon.application.conjugation.ChooseVerbImageUseCaseImpl
import com.lexicon.application.conjugation.CreateConjugationCourseUseCaseImpl
import com.lexicon.application.conjugation.DeleteConjugationCourseUseCaseImpl
import com.lexicon.application.conjugation.DeleteConjugationVerbUseCaseImpl
import com.lexicon.application.conjugation.EnsureVerbWordUseCaseImpl
import com.lexicon.application.conjugation.HasDeletedVerbsUseCaseImpl
import com.lexicon.application.conjugation.LoadConjugationCoursesUseCaseImpl
import com.lexicon.application.conjugation.LoadConjugationProgressUseCaseImpl
import com.lexicon.application.conjugation.LoadConjugationVerbsUseCaseImpl
import com.lexicon.application.conjugation.LoadStudySetVerbsUseCaseImpl
import com.lexicon.application.conjugation.LoadVerbImageChoicesUseCaseImpl
import com.lexicon.application.conjugation.NextConjugationTableUseCaseImpl
import com.lexicon.application.conjugation.RestoreConjugationVerbsUseCaseImpl
import com.lexicon.application.conjugation.SubmitConjugationAnswerUseCaseImpl
import com.lexicon.application.conjugation.ToggleVerbInStudySetUseCaseImpl
import com.lexicon.application.course.CheckExerciseAnswerUseCaseImpl
import com.lexicon.application.course.GetLessonUseCaseImpl
import com.lexicon.application.course.GetLessonVocabularyUseCaseImpl
import com.lexicon.application.course.ObserveCoursesUseCaseImpl
import com.lexicon.application.course.SetLessonCompletedUseCaseImpl
import com.lexicon.application.crossword.StartCrosswordSessionUseCaseImpl
import com.lexicon.application.crossword.SubmitCrosswordUseCaseImpl
import com.lexicon.application.dictation.AnswerNormalizer
import com.lexicon.application.dictation.StartDictationSessionUseCaseImpl
import com.lexicon.application.dictation.SubmitDictationAnswerUseCaseImpl
import com.lexicon.application.dictationpuzzle.StartDictationPuzzleSessionUseCaseImpl
import com.lexicon.application.dictationpuzzle.SubmitDictationPuzzleAnswerUseCaseImpl
import com.lexicon.application.fillword.StartFillwordSessionUseCaseImpl
import com.lexicon.application.imagetest.StartImageTestSessionUseCaseImpl
import com.lexicon.application.imagetest.SubmitImageTestAnswerUseCaseImpl
import com.lexicon.application.memorycards.StartMemoryCardsSessionUseCaseImpl
import com.lexicon.application.memorycards.SubmitMemoryCardsStepResultUseCaseImpl
import com.lexicon.application.mix.StartMixSessionUseCaseImpl
import com.lexicon.application.passage.StartPassageSessionUseCaseImpl
import com.lexicon.application.passage.SubmitPassageAnswersUseCaseImpl
import com.lexicon.application.presets.CreatePresetUseCaseImpl
import com.lexicon.application.presets.CreateWordUseCaseImpl
import com.lexicon.application.presets.DeletePresetUseCaseImpl
import com.lexicon.application.presets.DeleteWordUseCaseImpl
import com.lexicon.application.presets.GetPinnedImageUseCaseImpl
import com.lexicon.application.presets.GetPresetCategoriesUseCaseImpl
import com.lexicon.application.presets.GetPresetVocabularyUseCaseImpl
import com.lexicon.application.presets.GetVocabularyPresetUseCaseImpl
import com.lexicon.application.presets.GetVocabularyPresetsUseCaseImpl
import com.lexicon.application.presets.GetWordPresetMembershipsUseCaseImpl
import com.lexicon.application.presets.GetWordUseCaseImpl
import com.lexicon.application.presets.ObserveStudySetIdsUseCaseImpl
import com.lexicon.application.presets.ObserveVocabularyPresetsUseCaseImpl
import com.lexicon.application.presets.RestorePresetUseCaseImpl
import com.lexicon.application.presets.RestoreWordUseCaseImpl
import com.lexicon.application.presets.SearchImageCandidatesUseCaseImpl
import com.lexicon.application.presets.SearchVocabularyUseCaseImpl
import com.lexicon.application.presets.SetPresetInStudySetUseCaseImpl
import com.lexicon.application.presets.SetWordPresetMembershipUseCaseImpl
import com.lexicon.application.presets.ToggleWordInStudySetUseCaseImpl
import com.lexicon.application.presets.TranslateWordUseCaseImpl
import com.lexicon.application.presets.UpdateWordUseCaseImpl
import com.lexicon.application.program.AdvanceProgramDayUseCaseImpl
import com.lexicon.application.program.CountStudySetUseCaseImpl
import com.lexicon.application.program.CreateProgramUseCaseImpl
import com.lexicon.application.program.EnrolInProgramUseCaseImpl
import com.lexicon.application.program.GetProgramDayUseCaseImpl
import com.lexicon.application.program.GetProgramProgressUseCaseImpl
import com.lexicon.application.program.GetProgramUseCaseImpl
import com.lexicon.application.program.GetStudyStreakUseCaseImpl
import com.lexicon.application.program.GetWordCardsUseCaseImpl
import com.lexicon.application.program.LeaveProgramUseCaseImpl
import com.lexicon.application.program.MarkCardsSeenUseCaseImpl
import com.lexicon.application.program.NextProgramTrainingUseCaseImpl
import com.lexicon.application.program.ObserveActiveEnrolmentUseCaseImpl
import com.lexicon.application.program.ObserveProgramsUseCaseImpl
import com.lexicon.application.program.ResolveProgramScopeUseCaseImpl
import com.lexicon.application.program.StartProgramSessionUseCaseImpl
import com.lexicon.application.program.UpdateProgramUseCaseImpl
import com.lexicon.application.pronunciation.StartPronunciationSentencesUseCaseImpl
import com.lexicon.application.pronunciation.StartPronunciationSessionUseCaseImpl
import com.lexicon.application.pronunciation.SubmitPronunciationResultUseCaseImpl
import com.lexicon.application.puzzle.StartPuzzleSessionUseCaseImpl
import com.lexicon.application.puzzle.SubmitPuzzleAnswerUseCaseImpl
import com.lexicon.application.settings.ObserveSettingsUseCaseImpl
import com.lexicon.application.settings.StepCountResolver
import com.lexicon.application.settings.UpdateStepCountUseCaseImpl
import com.lexicon.application.settings.UpdateThemeModeUseCaseImpl
import com.lexicon.application.settings.UpdateVoiceUseCaseImpl
import com.lexicon.application.sync.SeedCatalogsUseCaseImpl
import com.lexicon.application.training.CheckTrainingReadinessUseCaseImpl
import com.lexicon.application.training.RecordAnswerUseCaseImpl
import com.lexicon.application.trueorfalse.StartTrueOrFalseSessionUseCaseImpl
import com.lexicon.application.trueorfalse.SubmitTrueOrFalseAnswerUseCaseImpl
import com.lexicon.application.wordcard.RecordWordCardSeenUseCaseImpl
import com.lexicon.application.wordcard.StartWordCardSessionUseCaseImpl
import com.lexicon.application.wordmatch.StartWordMatchSessionUseCaseImpl
import com.lexicon.application.wordmatch.SubmitWordMatchStepResultUseCaseImpl
import com.lexicon.interactors.conjugation.ChooseVerbImageUseCase
import com.lexicon.interactors.conjugation.CreateConjugationCourseUseCase
import com.lexicon.interactors.conjugation.DeleteConjugationCourseUseCase
import com.lexicon.interactors.conjugation.DeleteConjugationVerbUseCase
import com.lexicon.interactors.conjugation.EnsureVerbWordUseCase
import com.lexicon.interactors.conjugation.HasDeletedVerbsUseCase
import com.lexicon.interactors.conjugation.LoadConjugationCoursesUseCase
import com.lexicon.interactors.conjugation.LoadConjugationProgressUseCase
import com.lexicon.interactors.conjugation.LoadConjugationVerbsUseCase
import com.lexicon.interactors.conjugation.LoadStudySetVerbsUseCase
import com.lexicon.interactors.conjugation.LoadVerbImageChoicesUseCase
import com.lexicon.interactors.conjugation.NextConjugationTableUseCase
import com.lexicon.interactors.conjugation.RestoreConjugationVerbsUseCase
import com.lexicon.interactors.conjugation.SubmitConjugationAnswerUseCase
import com.lexicon.interactors.conjugation.ToggleVerbInStudySetUseCase
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
import com.lexicon.interactors.presets.ObserveStudySetIdsUseCase
import com.lexicon.interactors.presets.ObserveVocabularyPresetsUseCase
import com.lexicon.interactors.presets.RestorePresetUseCase
import com.lexicon.interactors.presets.RestoreWordUseCase
import com.lexicon.interactors.presets.SearchImageCandidatesUseCase
import com.lexicon.interactors.presets.SearchVocabularyUseCase
import com.lexicon.interactors.presets.SetPresetInStudySetUseCase
import com.lexicon.interactors.presets.SetWordPresetMembershipUseCase
import com.lexicon.interactors.presets.ToggleWordInStudySetUseCase
import com.lexicon.interactors.presets.TranslateWordUseCase
import com.lexicon.interactors.presets.UpdateWordUseCase
import com.lexicon.interactors.program.AdvanceProgramDayUseCase
import com.lexicon.interactors.program.CountStudySetUseCase
import com.lexicon.interactors.program.CreateProgramUseCase
import com.lexicon.interactors.program.EnrolInProgramUseCase
import com.lexicon.interactors.program.GetProgramDayUseCase
import com.lexicon.interactors.program.GetProgramProgressUseCase
import com.lexicon.interactors.program.GetProgramUseCase
import com.lexicon.interactors.program.GetStudyStreakUseCase
import com.lexicon.interactors.program.GetWordCardsUseCase
import com.lexicon.interactors.program.LeaveProgramUseCase
import com.lexicon.interactors.program.MarkCardsSeenUseCase
import com.lexicon.interactors.program.NextProgramTrainingUseCase
import com.lexicon.interactors.program.ObserveActiveEnrolmentUseCase
import com.lexicon.interactors.program.ObserveProgramsUseCase
import com.lexicon.interactors.program.ResolveProgramScopeUseCase
import com.lexicon.interactors.program.StartProgramSessionUseCase
import com.lexicon.interactors.program.UpdateProgramUseCase
import com.lexicon.interactors.pronunciation.StartPronunciationSentencesUseCase
import com.lexicon.interactors.pronunciation.StartPronunciationSessionUseCase
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultUseCase
import com.lexicon.interactors.puzzle.StartPuzzleSessionUseCase
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerUseCase
import com.lexicon.interactors.settings.ObserveSettingsUseCase
import com.lexicon.interactors.settings.UpdateStepCountUseCase
import com.lexicon.interactors.settings.UpdateThemeModeUseCase
import com.lexicon.interactors.settings.UpdateVoiceUseCase
import com.lexicon.interactors.sync.SeedCatalogsUseCase
import com.lexicon.interactors.training.CheckTrainingReadinessUseCase
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.trueorfalse.StartTrueOrFalseSessionUseCase
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerUseCase
import com.lexicon.interactors.wordcard.RecordWordCardSeenUseCase
import com.lexicon.interactors.wordcard.StartWordCardSessionUseCase
import com.lexicon.interactors.wordmatch.StartWordMatchSessionUseCase
import com.lexicon.interactors.wordmatch.SubmitWordMatchStepResultUseCase
import com.lexicon.model.scheduling.ReviewSettings
import com.lexicon.model.scheduling.StudyTimePolicy
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val domainModule = module {
    single { ReviewSettings() }
    single { StudyTimePolicy() }

    factoryOf(::NextProgramTrainingUseCaseImpl) { bind<NextProgramTrainingUseCase>() }
    factoryOf(::AnswerNormalizer)
    factoryOf(::RecordAnswerUseCaseImpl) { bind<RecordAnswerUseCase>() }
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
    factoryOf(::ToggleWordInStudySetUseCaseImpl) { bind<ToggleWordInStudySetUseCase>() }
    factoryOf(::SetPresetInStudySetUseCaseImpl) { bind<SetPresetInStudySetUseCase>() }
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
    factoryOf(::CountStudySetUseCaseImpl) { bind<CountStudySetUseCase>() }
    factoryOf(::GetProgramDayUseCaseImpl) { bind<GetProgramDayUseCase>() }
    factoryOf(::AdvanceProgramDayUseCaseImpl) { bind<AdvanceProgramDayUseCase>() }
    factoryOf(::StartPassageSessionUseCaseImpl) { bind<StartPassageSessionUseCase>() }
    factoryOf(::StartFillwordSessionUseCaseImpl) { bind<StartFillwordSessionUseCase>() }
    factoryOf(::StartPronunciationSentencesUseCaseImpl) { bind<StartPronunciationSentencesUseCase>() }

    factoryOf(::LoadConjugationVerbsUseCaseImpl) { bind<LoadConjugationVerbsUseCase>() }
    factoryOf(::DeleteConjugationVerbUseCaseImpl) { bind<DeleteConjugationVerbUseCase>() }
    factoryOf(::RestoreConjugationVerbsUseCaseImpl) { bind<RestoreConjugationVerbsUseCase>() }
    factoryOf(::HasDeletedVerbsUseCaseImpl) { bind<HasDeletedVerbsUseCase>() }
    factoryOf(::NextConjugationTableUseCaseImpl) { bind<NextConjugationTableUseCase>() }
    factoryOf(::SubmitConjugationAnswerUseCaseImpl) { bind<SubmitConjugationAnswerUseCase>() }
    factoryOf(::LoadConjugationProgressUseCaseImpl) { bind<LoadConjugationProgressUseCase>() }
    factoryOf(::CreateConjugationCourseUseCaseImpl) { bind<CreateConjugationCourseUseCase>() }
    factoryOf(::LoadConjugationCoursesUseCaseImpl) { bind<LoadConjugationCoursesUseCase>() }
    factoryOf(::DeleteConjugationCourseUseCaseImpl) { bind<DeleteConjugationCourseUseCase>() }
    factoryOf(::EnsureVerbWordUseCaseImpl) { bind<EnsureVerbWordUseCase>() }
    factoryOf(::LoadVerbImageChoicesUseCaseImpl) { bind<LoadVerbImageChoicesUseCase>() }
    factoryOf(::ChooseVerbImageUseCaseImpl) { bind<ChooseVerbImageUseCase>() }
    factoryOf(::ToggleVerbInStudySetUseCaseImpl) { bind<ToggleVerbInStudySetUseCase>() }
    factoryOf(::LoadStudySetVerbsUseCaseImpl) { bind<LoadStudySetVerbsUseCase>() }
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
    factoryOf(::ObserveStudySetIdsUseCaseImpl) { bind<ObserveStudySetIdsUseCase>() }
    factoryOf(::CheckTrainingReadinessUseCaseImpl) { bind<CheckTrainingReadinessUseCase>() }
    factoryOf(::SearchVocabularyUseCaseImpl) { bind<SearchVocabularyUseCase>() }
    factoryOf(::SeedCatalogsUseCaseImpl) { bind<SeedCatalogsUseCase>() }
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

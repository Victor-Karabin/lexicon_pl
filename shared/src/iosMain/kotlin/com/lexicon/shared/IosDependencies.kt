package com.lexicon.shared

import com.lexicon.interactors.course.CheckExerciseAnswerUseCase
import com.lexicon.interactors.course.Course
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
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.presets.VocabularyPreset
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
import com.lexicon.interactors.program.Program
import com.lexicon.interactors.program.ProgramEnrolment
import com.lexicon.interactors.program.StartProgramSessionUseCase
import com.lexicon.interactors.program.UpdateProgramUseCase
import com.lexicon.interactors.pronunciation.StartPronunciationSessionUseCase
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultUseCase
import com.lexicon.interactors.puzzle.StartPuzzleSessionUseCase
import com.lexicon.interactors.puzzle.SubmitPuzzleAnswerUseCase
import com.lexicon.interactors.settings.AppSettings
import com.lexicon.interactors.settings.ObserveSettingsUseCase
import com.lexicon.interactors.settings.UpdateStepCountUseCase
import com.lexicon.interactors.settings.UpdateThemeModeUseCase
import com.lexicon.interactors.sync.CatalogSyncStatus
import com.lexicon.interactors.sync.SyncCatalogUseCase
import com.lexicon.interactors.training.CheckTrainingReadinessUseCase
import com.lexicon.interactors.trueorfalse.StartTrueOrFalseSessionUseCase
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerUseCase
import com.lexicon.interactors.wordcard.RecordWordCardSeenUseCase
import com.lexicon.interactors.wordcard.StartWordCardSessionUseCase
import com.lexicon.interactors.wordmatch.StartWordMatchSessionUseCase
import com.lexicon.interactors.wordmatch.SubmitWordMatchStepResultUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("TooManyFunctions")
object IosDependencies : KoinComponent {
    val syncCatalog: SyncCatalogUseCase by inject()
    val observeSettings: ObserveSettingsUseCase by inject()
    val updateThemeMode: UpdateThemeModeUseCase by inject()
    val updateStepCount: UpdateStepCountUseCase by inject()

    val getPresets: GetVocabularyPresetsUseCase by inject()
    val observePresets: ObserveVocabularyPresetsUseCase by inject()
    val getPreset: GetVocabularyPresetUseCase by inject()
    val getPresetCategories: GetPresetCategoriesUseCase by inject()
    val getPresetVocabulary: GetPresetVocabularyUseCase by inject()
    val searchVocabulary: SearchVocabularyUseCase by inject()
    val toggleWordFavourite: ToggleWordFavouriteUseCase by inject()
    val setPresetFavourite: SetPresetFavouriteUseCase by inject()
    val observeFavouriteWordIds: ObserveFavouriteWordIdsUseCase by inject()
    val getWordPresetMemberships: GetWordPresetMembershipsUseCase by inject()
    val setWordPresetMembership: SetWordPresetMembershipUseCase by inject()

    val createWord: CreateWordUseCase by inject()
    val updateWord: UpdateWordUseCase by inject()
    val getWord: GetWordUseCase by inject()
    val deleteWord: DeleteWordUseCase by inject()
    val restoreWord: RestoreWordUseCase by inject()
    val createPreset: CreatePresetUseCase by inject()
    val deletePreset: DeletePresetUseCase by inject()
    val restorePreset: RestorePresetUseCase by inject()
    val translateWord: TranslateWordUseCase by inject()
    val searchImageCandidates: SearchImageCandidatesUseCase by inject()
    val getPinnedImage: GetPinnedImageUseCase by inject()

    val observeCourses: ObserveCoursesUseCase by inject()
    val getLesson: GetLessonUseCase by inject()
    val getLessonVocabulary: GetLessonVocabularyUseCase by inject()
    val setLessonCompleted: SetLessonCompletedUseCase by inject()
    val checkExerciseAnswer: CheckExerciseAnswerUseCase by inject()

    val observePrograms: ObserveProgramsUseCase by inject()
    val getProgram: GetProgramUseCase by inject()
    val createProgram: CreateProgramUseCase by inject()
    val updateProgram: UpdateProgramUseCase by inject()
    val countFavourites: CountFavouritesUseCase by inject()
    val observeActiveEnrolment: ObserveActiveEnrolmentUseCase by inject()
    val enrolInProgram: EnrolInProgramUseCase by inject()
    val leaveProgram: LeaveProgramUseCase by inject()
    val startProgramSession: StartProgramSessionUseCase by inject()
    val getProgramProgress: GetProgramProgressUseCase by inject()
    val getStudyStreak: GetStudyStreakUseCase by inject()
    val getProgramDay: GetProgramDayUseCase by inject()
    val markCardsSeen: MarkCardsSeenUseCase by inject()
    val getWordCards: GetWordCardsUseCase by inject()

    val checkTrainingReadiness: CheckTrainingReadinessUseCase by inject()

    val startDictation: StartDictationSessionUseCase by inject()
    val submitDictation: SubmitDictationAnswerUseCase by inject()
    val startDictationPuzzle: StartDictationPuzzleSessionUseCase by inject()
    val submitDictationPuzzle: SubmitDictationPuzzleAnswerUseCase by inject()
    val startPuzzle: StartPuzzleSessionUseCase by inject()
    val submitPuzzle: SubmitPuzzleAnswerUseCase by inject()
    val startImageTest: StartImageTestSessionUseCase by inject()
    val submitImageTest: SubmitImageTestAnswerUseCase by inject()
    val startWordMatch: StartWordMatchSessionUseCase by inject()
    val submitWordMatch: SubmitWordMatchStepResultUseCase by inject()
    val startTrueOrFalse: StartTrueOrFalseSessionUseCase by inject()
    val submitTrueOrFalse: SubmitTrueOrFalseAnswerUseCase by inject()
    val startPronunciation: StartPronunciationSessionUseCase by inject()
    val submitPronunciation: SubmitPronunciationResultUseCase by inject()
    val startMemoryCards: StartMemoryCardsSessionUseCase by inject()
    val submitMemoryCards: SubmitMemoryCardsStepResultUseCase by inject()
    val startCrossword: StartCrosswordSessionUseCase by inject()
    val submitCrossword: SubmitCrosswordUseCase by inject()
    val startMix: StartMixSessionUseCase by inject()
    val startWordCard: StartWordCardSessionUseCase by inject()
    val recordWordCardSeen: RecordWordCardSeenUseCase by inject()

    fun watchCatalogSync(onEach: (CatalogSyncStatus) -> Unit): Cancellable = syncCatalog().watch(onEach)

    fun watchSettings(onEach: (AppSettings) -> Unit): Cancellable = observeSettings().watch(onEach)

    fun watchPresets(onEach: (List<VocabularyPreset>) -> Unit): Cancellable = observePresets().watch(onEach)

    fun watchFavouriteWordIds(onEach: (Set<VocabularyId>) -> Unit): Cancellable = observeFavouriteWordIds().watch(onEach)

    fun watchCourses(onEach: (List<Course>) -> Unit): Cancellable = observeCourses().watch(onEach)

    fun watchPrograms(onEach: (List<Program>) -> Unit): Cancellable = observePrograms().watch(onEach)

    fun watchActiveEnrolment(onEach: (ProgramEnrolment?) -> Unit): Cancellable = observeActiveEnrolment().watch(onEach)

    private fun <T> Flow<T>.watch(onEach: (T) -> Unit): Cancellable {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val job = scope.launch { collect { onEach(it) } }
        return Cancellable(job)
    }
}

class Cancellable(private val job: Job) {
    fun cancel() {
        job.cancel()
    }
}

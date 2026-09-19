package com.lexicon.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.interactors.conjugation.ConjugationCourse
import com.lexicon.interactors.conjugation.DeleteConjugationCourseUseCase
import com.lexicon.interactors.conjugation.LoadConjugationCoursesUseCase
import com.lexicon.interactors.presets.CountWordsToReviewUseCase
import com.lexicon.interactors.vocabularycourse.GetCourseProgressUseCase
import com.lexicon.interactors.vocabularycourse.GetDailyStudyTimeUseCase
import com.lexicon.interactors.vocabularycourse.GetStudyStreakUseCase
import com.lexicon.interactors.vocabularycourse.NextCourseTrainingUseCase
import com.lexicon.interactors.vocabularycourse.ObserveVocabularyCourseUseCase
import com.lexicon.interactors.vocabularycourse.StudyTimeHistory
import com.lexicon.interactors.vocabularycourse.VocabularyCourse
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabularycourse.CourseProgress
import com.lexicon.model.vocabularycourse.ProgressMetricType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LaunchTraining(
    val training: TrainingType,
    val wordIds: ImmutableList<VocabularyId>,
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val course: VocabularyCourse? = null,
    val progress: CourseProgress? = null,
    val streakDays: Int = 0,
    val wordsToReview: Int = 0,
    val launch: LaunchTraining? = null,
    val openCards: Boolean = false,
    val nothingToPractise: Boolean = false,
    val studyTime: StudyTimeHistory? = null,
    val conjugationCourses: ImmutableList<ConjugationCourse> = persistentListOf(),
) {
    val knownWords: Int get() = progress?.metric(ProgressMetricType.VOCABULARY)?.current ?: 0

    val learningWords: Int
        get() = progress?.metric(ProgressMetricType.VOCABULARY)?.let { it.target - it.current } ?: 0
}

class DashboardViewModel(
    private val getProgress: GetCourseProgressUseCase,
    private val queue: NextCourseTrainingUseCase,
    private val loadConjugationCourses: LoadConjugationCoursesUseCase,
    private val deleteConjugationCourse: DeleteConjugationCourseUseCase,
    private val getStreak: GetStudyStreakUseCase,
    private val countWordsToReview: CountWordsToReviewUseCase,
    private val getDailyStudyTime: GetDailyStudyTimeUseCase,
    observeCourse: ObserveVocabularyCourseUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeCourse().collect { course ->
                val progress = getProgress()
                val courses = loadConjugationCourses()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        course = course,
                        progress = progress,
                        conjugationCourses = courses,
                        nothingToPractise = false,
                    )
                }
            }
        }
    }

    fun onResumed() {
        viewModelScope.launch {
            val studyTime = getDailyStudyTime()
            val wordsToReview = countWordsToReview()
            val streakDays = getStreak()
            val progress = getProgress()
            _uiState.update {
                it.copy(
                    studyTime = studyTime,
                    wordsToReview = wordsToReview,
                    streakDays = streakDays,
                    progress = progress,
                    nothingToPractise = false,
                )
            }
        }
    }

    fun onContinue() {
        val course = _uiState.value.course ?: return

        if (course.showCardsNext) {
            _uiState.update { it.copy(openCards = true) }
            return
        }
        viewModelScope.launch {
            val next = queue.next()
            _uiState.update {
                when (next) {
                    null -> it.copy(nothingToPractise = true)
                    else -> it.copy(launch = LaunchTraining(next.training, next.wordIds))
                }
            }
        }
    }

    fun onLaunchHandled() = _uiState.update { it.copy(launch = null, openCards = false) }

    fun refreshConjugation() {
        viewModelScope.launch {
            _uiState.update { it.copy(conjugationCourses = loadConjugationCourses()) }
        }
    }

    fun onConjugationCourseRemoved(courseId: String) {
        viewModelScope.launch {
            deleteConjugationCourse(courseId)
            _uiState.update { it.copy(conjugationCourses = loadConjugationCourses()) }
        }
    }
}

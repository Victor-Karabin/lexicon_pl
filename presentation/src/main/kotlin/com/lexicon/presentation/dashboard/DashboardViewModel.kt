package com.lexicon.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.interactors.conjugation.ConjugationCourse
import com.lexicon.interactors.conjugation.LoadConjugationCoursesUseCase
import com.lexicon.interactors.countries.CountryCourse
import com.lexicon.interactors.countries.LoadCountryCourseUseCase
import com.lexicon.interactors.course.ObserveCoursesUseCase
import com.lexicon.interactors.declension.DeclensionCourse
import com.lexicon.interactors.declension.LoadDeclensionCoursesUseCase
import com.lexicon.interactors.numbers.LoadNumberCourseUseCase
import com.lexicon.interactors.numbers.NumberCourse
import com.lexicon.interactors.presets.CountWordsToReviewUseCase
import com.lexicon.interactors.presets.ObserveWordStatusesUseCase
import com.lexicon.interactors.vocabularycourse.CourseLaunch
import com.lexicon.interactors.vocabularycourse.GetCourseProgressUseCase
import com.lexicon.interactors.vocabularycourse.GetDailyStudyTimeUseCase
import com.lexicon.interactors.vocabularycourse.NextCourseTrainingUseCase
import com.lexicon.interactors.vocabularycourse.ObserveVocabularyCourseUseCase
import com.lexicon.interactors.vocabularycourse.StudyTimeHistory
import com.lexicon.interactors.vocabularycourse.VocabularyCourse
import com.lexicon.model.course.Course
import com.lexicon.model.vocabularycourse.CourseProgress
import com.lexicon.model.vocabularycourse.ProgressMetricType
import com.lexicon.presentation.vocabularycourse.CourseRunStep
import com.lexicon.presentation.vocabularycourse.stepFor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val course: VocabularyCourse? = null,
    val progress: CourseProgress? = null,
    val wordsToReview: Int = 0,
    val launch: CourseLaunch? = null,
    val openCards: Boolean = false,
    val nothingToPractise: Boolean = false,
    val studyTime: StudyTimeHistory? = null,
    val conjugationCourses: ImmutableList<ConjugationCourse> = persistentListOf(),
    val declensionCourses: ImmutableList<DeclensionCourse> = persistentListOf(),
    val numberCourse: NumberCourse? = null,
    val countryCourse: CountryCourse? = null,
    val courses: ImmutableList<Course> = persistentListOf(),
    val languageTag: String = "en",
) {
    val accuracy: Int? get() = progress?.metric(ProgressMetricType.ACCURACY)?.takeIf { it.isMeasured }?.current

    val dueWords: Int? get() = progress?.dueWords?.takeIf { it > 0 }

    val knownWords: Int get() = progress?.metric(ProgressMetricType.VOCABULARY)?.current ?: 0

    val learningWords: Int
        get() = progress?.metric(ProgressMetricType.VOCABULARY)?.let { it.target - it.current } ?: 0
}

private data class CourseGroups(
    val conjugation: ImmutableList<ConjugationCourse>,
    val declension: ImmutableList<DeclensionCourse>,
    val numbers: NumberCourse?,
    val countries: CountryCourse?,
)

private fun DashboardUiState.with(groups: CourseGroups): DashboardUiState =
    copy(
        conjugationCourses = groups.conjugation,
        declensionCourses = groups.declension,
        numberCourse = groups.numbers,
        countryCourse = groups.countries,
    )

class DashboardViewModel(
    private val getProgress: GetCourseProgressUseCase,
    private val queue: NextCourseTrainingUseCase,
    private val loadConjugationCourses: LoadConjugationCoursesUseCase,
    private val loadDeclensionCourses: LoadDeclensionCoursesUseCase,
    private val loadNumberCourse: LoadNumberCourseUseCase,
    private val loadCountryCourse: LoadCountryCourseUseCase,
    private val countWordsToReview: CountWordsToReviewUseCase,
    private val getDailyStudyTime: GetDailyStudyTimeUseCase,
    observeCourse: ObserveVocabularyCourseUseCase,
    observeCourses: ObserveCoursesUseCase,
    observeWordStatuses: ObserveWordStatusesUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeCourse().collect { course ->
                val progress = getProgress()
                val groups = loadCourseGroups()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        course = course,
                        progress = progress,
                        nothingToPractise = false,
                    ).with(groups)
                }
            }
        }
        viewModelScope.launch {
            observeWordStatuses().drop(1).collect { refreshCourses() }
        }
        viewModelScope.launch {
            observeCourses().collect { courses ->
                _uiState.update { it.copy(courses = courses.filter { course -> course.isOnDashboard }.toImmutableList()) }
            }
        }
    }

    fun onResumed() {
        viewModelScope.launch {
            val studyTime = getDailyStudyTime()
            val wordsToReview = countWordsToReview()
            val progress = getProgress()
            _uiState.update {
                it.copy(
                    studyTime = studyTime,
                    wordsToReview = wordsToReview,
                    progress = progress,
                    nothingToPractise = false,
                )
            }
        }
    }

    fun onContinue() {
        val course = _uiState.value.course ?: return

        viewModelScope.launch {
            val step = queue.stepFor(course)
            _uiState.update {
                when (step) {
                    CourseRunStep.Cards -> it.copy(openCards = true)
                    is CourseRunStep.Next -> it.copy(launch = CourseLaunch(step.training, step.wordIds))
                    else -> it.copy(nothingToPractise = true)
                }
            }
        }
    }

    fun onLaunchHandled() = _uiState.update { it.copy(launch = null, openCards = false) }

    fun refreshCourses() {
        viewModelScope.launch {
            val groups = loadCourseGroups()
            _uiState.update { it.with(groups) }
        }
    }

    private suspend fun loadCourseGroups(): CourseGroups =
        CourseGroups(
            conjugation = loadConjugationCourses(),
            declension = loadDeclensionCourses(),
            numbers = loadNumberCourse(),
            countries = loadCountryCourse(),
        )
}

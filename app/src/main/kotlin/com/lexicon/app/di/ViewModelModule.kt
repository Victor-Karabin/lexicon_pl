package com.lexicon.app.di

import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionResultViewModel
import com.lexicon.presentation.common.TrainingGateViewModel
import com.lexicon.presentation.course.CourseDetailViewModel
import com.lexicon.presentation.course.ExerciseViewModel
import com.lexicon.presentation.course.LessonViewModel
import com.lexicon.presentation.course.PlanViewModel
import com.lexicon.presentation.crossword.CrosswordViewModel
import com.lexicon.presentation.dictation.DictationViewModel
import com.lexicon.presentation.dictationpuzzle.DictationPuzzleViewModel
import com.lexicon.presentation.imagetest.ImageTestViewModel
import com.lexicon.presentation.main.SplashViewModel
import com.lexicon.presentation.memorycards.MemoryCardsViewModel
import com.lexicon.presentation.mix.MixViewModel
import com.lexicon.presentation.presets.CreatePresetViewModel
import com.lexicon.presentation.presets.CreateWordViewModel
import com.lexicon.presentation.presets.PresetDetailViewModel
import com.lexicon.presentation.presets.VocabularyViewModel
import com.lexicon.presentation.program.ProgramViewModel
import com.lexicon.presentation.pronunciation.PronunciationViewModel
import com.lexicon.presentation.puzzle.PuzzleViewModel
import com.lexicon.presentation.settings.SettingsViewModel
import com.lexicon.presentation.trueorfalse.TrueOrFalseViewModel
import com.lexicon.presentation.wordmatch.WordMatchViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    singleOf(::LastSessionResultsHolder)

    viewModelOf(::SessionResultViewModel)
    viewModelOf(::TrainingGateViewModel)
    viewModelOf(::PlanViewModel)
    viewModelOf(::ProgramViewModel)
    viewModelOf(::CourseDetailViewModel)
    viewModelOf(::ExerciseViewModel)
    viewModelOf(::LessonViewModel)
    viewModelOf(::CrosswordViewModel)
    viewModelOf(::DictationViewModel)
    viewModelOf(::DictationPuzzleViewModel)
    viewModelOf(::ImageTestViewModel)
    viewModelOf(::SplashViewModel)
    viewModelOf(::MemoryCardsViewModel)
    viewModelOf(::MixViewModel)
    viewModelOf(::PresetDetailViewModel)
    viewModelOf(::CreateWordViewModel)
    viewModelOf(::CreatePresetViewModel)
    viewModelOf(::VocabularyViewModel)
    viewModelOf(::PronunciationViewModel)
    viewModelOf(::PuzzleViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::TrueOrFalseViewModel)
    viewModelOf(::WordMatchViewModel)
}

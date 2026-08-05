package com.lexicon.pl.app.di

import com.lexicon.pl.domain.dictation.StartDictationSessionUseCaseImpl
import com.lexicon.pl.domain.dictation.SubmitDictationAnswerUseCaseImpl
import com.lexicon.pl.domain.dictationpuzzle.StartDictationPuzzleSessionUseCaseImpl
import com.lexicon.pl.domain.dictationpuzzle.SubmitDictationPuzzleAnswerUseCaseImpl
import com.lexicon.pl.domain.pronunciation.StartPronunciationSessionUseCaseImpl
import com.lexicon.pl.domain.pronunciation.SubmitPronunciationResultUseCaseImpl
import com.lexicon.pl.domain.trueorfalse.StartTrueOrFalseSessionUseCaseImpl
import com.lexicon.pl.domain.trueorfalse.SubmitTrueOrFalseAnswerUseCaseImpl
import com.lexicon.pl.domain.wordbuilder.StartWordBuilderSessionUseCaseImpl
import com.lexicon.pl.domain.wordbuilder.SubmitWordBuilderAnswerUseCaseImpl
import com.lexicon.pl.domain.wordmatch.StartWordMatchSessionUseCaseImpl
import com.lexicon.pl.domain.wordmatch.SubmitWordMatchStepResultUseCaseImpl
import com.lexicon.pl.interactors.dictation.StartDictationSessionUseCase
import com.lexicon.pl.interactors.dictation.SubmitDictationAnswerUseCase
import com.lexicon.pl.interactors.dictationpuzzle.StartDictationPuzzleSessionUseCase
import com.lexicon.pl.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerUseCase
import com.lexicon.pl.interactors.pronunciation.StartPronunciationSessionUseCase
import com.lexicon.pl.interactors.pronunciation.SubmitPronunciationResultUseCase
import com.lexicon.pl.interactors.trueorfalse.StartTrueOrFalseSessionUseCase
import com.lexicon.pl.interactors.trueorfalse.SubmitTrueOrFalseAnswerUseCase
import com.lexicon.pl.interactors.wordbuilder.StartWordBuilderSessionUseCase
import com.lexicon.pl.interactors.wordbuilder.SubmitWordBuilderAnswerUseCase
import com.lexicon.pl.interactors.wordmatch.StartWordMatchSessionUseCase
import com.lexicon.pl.interactors.wordmatch.SubmitWordMatchStepResultUseCase
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
    abstract fun bindStartWordBuilderSessionUseCase(impl: StartWordBuilderSessionUseCaseImpl): StartWordBuilderSessionUseCase

    @Binds
    abstract fun bindSubmitWordBuilderAnswerUseCase(impl: SubmitWordBuilderAnswerUseCaseImpl): SubmitWordBuilderAnswerUseCase

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
}

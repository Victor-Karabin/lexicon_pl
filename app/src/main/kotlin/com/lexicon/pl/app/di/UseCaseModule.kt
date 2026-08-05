package com.lexicon.pl.app.di

import com.lexicon.pl.domain.dictation.StartDictationSessionUseCaseImpl
import com.lexicon.pl.domain.dictation.SubmitDictationAnswerUseCaseImpl
import com.lexicon.pl.interactors.dictation.StartDictationSessionUseCase
import com.lexicon.pl.interactors.dictation.SubmitDictationAnswerUseCase
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
}

package com.lexicon.interactors.program

interface GetDailyStudyTimeUseCase {
    suspend operator fun invoke(): StudyTimeHistory
}

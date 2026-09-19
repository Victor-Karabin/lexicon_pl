package com.lexicon.interactors.vocabularycourse

interface GetDailyStudyTimeUseCase {
    suspend operator fun invoke(): StudyTimeHistory
}

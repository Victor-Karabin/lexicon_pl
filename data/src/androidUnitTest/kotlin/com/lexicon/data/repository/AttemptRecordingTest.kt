package com.lexicon.data.repository

import com.lexicon.data.local.AssetReader
import com.lexicon.data.local.CatalogSeedStore
import com.lexicon.data.local.ConjugationAssetLoader
import com.lexicon.data.local.ConjugationDao
import com.lexicon.data.local.ConjugationProgressEntity
import com.lexicon.data.local.DeclensionDao
import com.lexicon.data.local.DeclensionProgressEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AttemptRecordingTest {
    private val declensionDao: DeclensionDao = mockk(relaxed = true)
    private val declension = DeclensionRepositoryImpl(declensionDao, mockk(relaxed = true))

    private val conjugationDao: ConjugationDao = mockk(relaxed = true)
    private val conjugation = ConjugationRepositoryImpl(
        ConjugationAssetLoader(mockk<AssetReader>(relaxed = true)),
        conjugationDao,
        mockk<CatalogSeedStore>(relaxed = true),
    )

    @Test
    fun `a declension answer extends or resets the streak and a first one starts the counts`() =
        runTest {
            coEvery { declensionDao.progressFor("c", "kot", "SINGULAR", "GENITIVE") } returns
                DeclensionProgressEntity("c", "kot", "SINGULAR", "GENITIVE", attempted = 3, correct = 2, incorrect = 1, streak = 2)
            coEvery { declensionDao.progressFor("c", "pies", "SINGULAR", "GENITIVE") } returns null

            declension.recordAttempt("c", "kot", "SINGULAR", "GENITIVE", isCorrect = true)
            coVerify { declensionDao.save(DeclensionProgressEntity("c", "kot", "SINGULAR", "GENITIVE", 4, 3, 1, 3)) }

            declension.recordAttempt("c", "kot", "SINGULAR", "GENITIVE", isCorrect = false)
            coVerify { declensionDao.save(DeclensionProgressEntity("c", "kot", "SINGULAR", "GENITIVE", 4, 2, 2, 0)) }

            declension.recordAttempt("c", "pies", "SINGULAR", "GENITIVE", isCorrect = true)
            coVerify { declensionDao.save(DeclensionProgressEntity("c", "pies", "SINGULAR", "GENITIVE", 1, 1, 0, 1)) }
        }

    @Test
    fun `a conjugation answer extends or resets the streak and a first one starts the counts`() =
        runTest {
            coEvery { conjugationDao.progressFor("c", "czytać", "ja") } returns
                ConjugationProgressEntity("c", "czytać", "ja", attempted = 3, correct = 2, incorrect = 1, streak = 2)
            coEvery { conjugationDao.progressFor("c", "czytać", "ty") } returns null

            conjugation.recordAttempt("c", "czytać", "ja", isCorrect = true)
            coVerify { conjugationDao.save(ConjugationProgressEntity("c", "czytać", "ja", 4, 3, 1, 3)) }

            conjugation.recordAttempt("c", "czytać", "ja", isCorrect = false)
            coVerify { conjugationDao.save(ConjugationProgressEntity("c", "czytać", "ja", 4, 2, 2, 0)) }

            conjugation.recordAttempt("c", "czytać", "ty", isCorrect = true)
            coVerify { conjugationDao.save(ConjugationProgressEntity("c", "czytać", "ty", 1, 1, 0, 1)) }
        }
}

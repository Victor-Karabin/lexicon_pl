package com.lexicon.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val OVER_THE_VARIABLE_LIMIT = 1_200

@RunWith(AndroidJUnit4::class)
class WordDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var words: WordDao

    @Before
    fun open() {
        database = Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        words = database.wordDao()
    }

    @After
    fun close() = database.close()

    private fun word(
        id: Long,
        text: String,
        translation: String,
        cefr: String = "A1",
        example: String = "",
    ) = WordEntity(
        id = id,
        text = text,
        translation = translation,
        transcription = "",
        searchKey = searchKeyFor(text, translation),
        cefr = cefr,
        example = example,
    )

    @Test
    fun aSearchIsFoldedSoAccentsDoNotHideAWord() =
        runTest {
            words.insertAll(listOf(word(1, "kość", "bone"), word(2, "kot", "cat")))

            val found = words.search(foldedQuery = "kosc", levels = emptyList(), ignoreLevels = 1, learningOnly = 0, limit = 10, offset = 0)

            assertEquals(listOf("kość"), found.map { it.text })
        }

    @Test
    fun pagesFollowOneAnotherWithoutRepeatingOrSkipping() =
        runTest {
            words.insertAll((1..25).map { word(it.toLong(), "word$it", "gloss$it") })

            val first = words.search(foldedQuery = "word", levels = emptyList(), ignoreLevels = 1, learningOnly = 0, limit = 10, offset = 0)
            val second = words.search(
                foldedQuery = "word",
                levels = emptyList(),
                ignoreLevels = 1,
                learningOnly = 0,
                limit = 10,
                offset = 10,
            )
            val third = words.search(
                foldedQuery = "word",
                levels = emptyList(),
                ignoreLevels = 1,
                learningOnly = 0,
                limit = 10,
                offset = 20,
            )

            assertEquals(10, first.size)
            assertEquals(10, second.size)
            assertEquals(5, third.size)
            assertTrue((first + second + third).map { it.id }.distinct().size == 25)
        }

    @Test
    fun aLevelFilterKeepsOnlyThatLevel() =
        runTest {
            words.insertAll(
                listOf(
                    word(1, "kot", "cat", cefr = "A1"),
                    word(2, "abstrakcyjny", "abstract", cefr = "B2"),
                ),
            )

            val found = words.search(foldedQuery = "", levels = listOf("B2"), ignoreLevels = 0, learningOnly = 0, limit = 10, offset = 0)

            assertEquals(listOf("abstrakcyjny"), found.map { it.text })
        }

    /**
     * SQLite takes 999 bound variables on Android 8 to 11, and a study set can be larger
     * than that. The batching this exercises is the reason starring a whole preset does
     * not crash there; the JVM tests cannot see it because they never bind to SQLite.
     */
    @Test
    fun aStudySetLargerThanSqlitesVariableLimitCanBeStarredAndReadBack() =
        runTest {
            val all = (1L..OVER_THE_VARIABLE_LIMIT).toList()
            words.insertAll(all.map { word(it, "word$it", "gloss$it") })

            all.forEachBatch { words.setStatus(it, "TO_LEARN") }

            assertEquals(OVER_THE_VARIABLE_LIMIT, words.studySetWordIds().size)
        }

    @Test
    fun askingWhichOfManyTextsAreStarredSurvivesTheSameLimit() =
        runTest {
            val all = (1L..OVER_THE_VARIABLE_LIMIT).toList()
            words.insertAll(all.map { word(it, "word$it", "gloss$it") })
            all.take(3).forEachBatch { words.setStatus(it, "TO_LEARN") }

            val texts = all.map { "word$it" }
            val starred = texts.inBatches { words.studySetTextsAmong(it) }

            assertEquals(setOf("word1", "word2", "word3"), starred.toSet())
        }

    @Test
    fun editingAWordKeepsTheExampleItWasGiven() =
        runTest {
            words.insertAll(listOf(word(1, "kobieta", "woman", example = "Ta **kobieta** jest miła.")))

            words.updateWord(
                id = 1,
                text = "kobieta",
                translation = "lady",
                transcription = "",
                searchKey = searchKeyFor("kobieta", "lady"),
                example = "Ta **kobieta** jest miła.",
            )

            assertEquals("Ta **kobieta** jest miła.", words.findById(1)?.example)
        }

    @Test
    fun aDeletedWordIsHiddenFromSearchButStillOnDisk() =
        runTest {
            words.insertAll(listOf(word(1, "kot", "cat")))

            words.setDeleted(1, true)

            assertTrue(
                words.search(
                    foldedQuery = "kot",
                    levels = emptyList(),
                    ignoreLevels = 1,
                    learningOnly = 0,
                    limit = 10,
                    offset = 0,
                ).isEmpty(),
            )
            assertEquals(1, words.countIncludingDeleted())
        }

    @Test
    fun favouritesComeBeforeWordsMerelyMarkedToLearn() =
        runTest {
            words.insertAll(
                listOf(
                    word(1L, "kot", "cat").copy(status = "TO_LEARN"),
                    word(2L, "pies", "dog").copy(status = "FAVOURITE"),
                    word(3L, "dom", "house").copy(status = "TO_LEARN"),
                    word(4L, "woda", "water").copy(status = "FAVOURITE"),
                    word(5L, "chleb", "bread").copy(status = "KNOWN"),
                    word(6L, "mleko", "milk").copy(status = "UNDEFINED"),
                ),
            )

            assertEquals(listOf(2L, 4L, 1L, 3L), words.learningWordIds(limit = 10))
        }

    @Test
    fun onlyKnownWordsAreDrawnForReview() =
        runTest {
            words.insertAll(
                listOf(
                    word(1L, "kot", "cat").copy(status = "TO_LEARN"),
                    word(2L, "pies", "dog").copy(status = "KNOWN"),
                    word(3L, "dom", "house").copy(status = "KNOWN"),
                ),
            )

            assertEquals(listOf(2L, 3L), words.randomKnownWordIds(limit = 10).sorted())
        }

    @Test
    fun aMeaningIsFoundByItsTranslationWhateverElseSharesItsLetters() =
        runTest {
            val crowd = (1L..60L).map { word(it, "aaa$it", "going $it") }
            words.insertAll(crowd + word(100, "iść", "to go") + word(101, "robić", "to do"))

            val found = words.withTranslationContaining("go", limit = 500).map { it.text }

            assertTrue("iść" in found)
            assertTrue(words.withTranslationContaining("do", limit = 500).any { it.text == "robić" })
        }

    @Test
    fun aPolishWordIsFoundByTheStartOfItsSearchKey() =
        runTest {
            words.insertAll(listOf(word(1, "dom", "house"), word(2, "domek", "little house"), word(3, "dom towarowy", "department store")))

            val found = words.withTextStarting("dom", limit = 500).map { it.text }.toSet()

            assertEquals(setOf("dom", "dom towarowy"), found)
        }
}

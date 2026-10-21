package com.lexicon.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val COURSE = "course-1"

@RunWith(AndroidJUnit4::class)
class ConjugationDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var verbs: ConjugationDao

    @Before
    fun open() {
        database = Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        verbs = database.conjugationDao()
    }

    @After
    fun close() = database.close()

    private fun verb(
        infinitive: String,
        isUserCreated: Boolean = false,
    ) = ConjugationVerbEntity(infinitive = infinitive, translation = "", formsJson = "{}", isUserCreated = isUserCreated)

    private fun tableNames(): List<String> =
        database.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
            generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }.toList()
        }

    private fun progress(
        infinitive: String,
        person: String,
        attempted: Int = 3,
        correct: Int = 2,
        incorrect: Int = 1,
        streak: Int = 2,
    ) = ConjugationProgressEntity(COURSE, infinitive, person, attempted, correct, incorrect, streak)

    @Test
    fun aVerbIsFoundByItsExactInfinitiveHoweverManyOthersContainIt() =
        runTest {
            verbs.saveVerbs(listOf("doznać", "poznać", "przyznać", "rozpoznać", "uznać", "wyznać", "znać").map(::verb))

            assertEquals("znać", verbs.verb("znać")?.infinitive)
            assertNull(verbs.verb("zna"))
        }

    @Test
    fun onlyBundledVerbsAreCountedAsTheCatalogue() =
        runTest {
            verbs.saveVerbs(listOf(verb("mieć"), verb("być"), verb("kotkować", isUserCreated = true)))

            assertEquals(3, verbs.countVerbs())
            assertEquals(2, verbs.countBundledVerbs())
        }

    @Test
    fun removingAUsersVerbNeverTakesABundledOne() =
        runTest {
            verbs.saveVerbs(listOf(verb("mieć"), verb("kotkować", isUserCreated = true)))

            verbs.deleteUserVerb("mieć")
            verbs.deleteUserVerb("kotkować")

            assertEquals("mieć", verbs.verb("mieć")?.infinitive)
            assertNull(verbs.verb("kotkować"))
        }

    @Test
    fun onlyTheVerbAndProgressTablesExist() {
        assertEquals(setOf("conjugation_verb", "conjugation_progress"), tableNames().filter { it.startsWith("conjugation_") }.toSet())
    }

    @Test
    fun aCoursesProgressIsSavedAndReadBack() =
        runTest {
            val ja = progress("czytać", "ja")
            val ty = progress("czytać", "ty", attempted = 1, correct = 1, incorrect = 0, streak = 1)
            verbs.save(ja)
            verbs.save(ty)

            assertEquals(setOf(ja, ty), verbs.progress(COURSE).toSet())
            assertEquals(ty, verbs.progressFor(COURSE, "czytać", "ty"))
            assertNull(verbs.progressFor(COURSE, "czytać", "on"))
        }

    @Test
    fun anotherAnswerReplacesTheCountsOfTheSamePerson() =
        runTest {
            verbs.save(progress("czytać", "ja"))
            val updated = progress("czytać", "ja", attempted = 4, correct = 3, incorrect = 1, streak = 3)
            verbs.save(updated)

            assertEquals(listOf(updated), verbs.progress(COURSE))
        }
}

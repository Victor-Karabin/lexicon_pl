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
}

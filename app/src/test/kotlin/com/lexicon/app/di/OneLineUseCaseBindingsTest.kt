package com.lexicon.app.di

import com.lexicon.application.di.domainModule
import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.ProgramRepository
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.conjugation.DeleteConjugationCourseUseCase
import com.lexicon.interactors.conjugation.DeleteConjugationVerbUseCase
import com.lexicon.interactors.conjugation.HasDeletedVerbsUseCase
import com.lexicon.interactors.conjugation.LoadStudySetVerbsUseCase
import com.lexicon.interactors.conjugation.RestoreConjugationVerbsUseCase
import com.lexicon.interactors.presets.DeletePresetUseCase
import com.lexicon.interactors.presets.DeleteWordUseCase
import com.lexicon.interactors.presets.GetWordUseCase
import com.lexicon.interactors.presets.RestorePresetUseCase
import com.lexicon.interactors.presets.RestoreWordUseCase
import com.lexicon.interactors.program.CountStudySetUseCase
import com.lexicon.interactors.program.DeleteProgramUseCase
import com.lexicon.interactors.settings.UpdateVoiceUseCase
import com.lexicon.model.program.ProgramId
import com.lexicon.model.vocabulary.PresetId
import com.lexicon.model.vocabulary.VocabularyId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

/**
 * A use case built as a lambda at the wiring site is invisible to Koin's `verify`, which
 * only reflects over constructors. These resolve each one from a real container and call
 * it, so a binding that asks for the wrong port, or forwards to the wrong repository
 * method, fails here rather than on a device.
 */
class OneLineUseCaseBindingsTest : KoinTest {
    private val vocabulary: VocabularyRepository = mockk(relaxed = true)
    private val presets: VocabularyPresetRepository = mockk(relaxed = true)
    private val conjugations: ConjugationRepository = mockk(relaxed = true)
    private val settings: SettingsRepository = mockk(relaxed = true)
    private val programs: ProgramRepository = mockk(relaxed = true)

    @Before
    fun start() {
        startKoin {
            modules(
                domainModule,
                module {
                    single { vocabulary }
                    single { presets }
                    single { conjugations }
                    single { settings }
                    single { programs }
                },
            )
        }
    }

    @After
    fun stop() = stopKoin()

    @Test
    fun `deleting and restoring a word reaches exactly that word`() =
        runTest {
            get<DeleteWordUseCase>()(VocabularyId(7L))
            get<RestoreWordUseCase>()(VocabularyId(7L))

            coVerify(exactly = 1) { vocabulary.deleteWord(7L) }
            coVerify(exactly = 1) { vocabulary.restoreWord(7L) }
        }

    @Test
    fun `deleting a preset leaves the words it listed alone`() =
        runTest {
            get<DeletePresetUseCase>()(PresetId("food"))

            coVerify(exactly = 1) { presets.deletePreset("food") }
            coVerify(exactly = 0) { vocabulary.deleteWord(any()) }
        }

    @Test
    fun `restoring a preset asks the catalogue to bring it back`() =
        runTest {
            get<RestorePresetUseCase>()(PresetId("food"))

            coVerify(exactly = 1) { presets.restorePreset("food") }
        }

    @Test
    fun `the study set count is the number of words in it`() =
        runTest {
            coEvery { vocabulary.studySetWordIds() } returns listOf(1L, 2L, 3L)

            assertEquals(3, get<CountStudySetUseCase>()())
        }

    @Test
    fun `a word is fetched by its id`() =
        runTest {
            get<GetWordUseCase>()(VocabularyId(12L))

            coVerify(exactly = 1) { vocabulary.getWord(12L) }
        }

    @Test
    fun `the verb use cases reach the conjugation store`() =
        runTest {
            get<DeleteConjugationVerbUseCase>()("robić")
            get<DeleteConjugationCourseUseCase>()("course-1")
            get<RestoreConjugationVerbsUseCase>()()
            get<HasDeletedVerbsUseCase>()()
            get<LoadStudySetVerbsUseCase>()(listOf("robić"))

            coVerify(exactly = 1) { conjugations.deleteVerb("robić") }
            coVerify(exactly = 1) { conjugations.deleteCourse("course-1") }
            coVerify(exactly = 1) { conjugations.restoreVerbs() }
            coVerify(exactly = 1) { conjugations.hasDeletedVerbs() }
            coVerify(exactly = 1) { vocabulary.studySetTextsAmong(listOf("robić")) }
        }

    @Test
    fun `choosing a voice writes it to settings`() =
        runTest {
            get<UpdateVoiceUseCase>()("pl-PL-Standard-A")

            coVerify(exactly = 1) { settings.setVoiceId("pl-PL-Standard-A") }
        }

    @Test
    fun `deleting a program reaches exactly that program`() =
        runTest {
            get<DeleteProgramUseCase>()(ProgramId("mine"))

            coVerify(exactly = 1) { programs.deleteProgram("mine") }
        }
}

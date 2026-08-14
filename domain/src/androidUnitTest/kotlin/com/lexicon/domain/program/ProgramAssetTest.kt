package com.lexicon.domain.program

import com.lexicon.interactors.program.ActivityType
import com.lexicon.interactors.program.ProgramConfig
import com.lexicon.interactors.program.ScopeSourceType
import com.lexicon.interactors.program.TargetType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The shipped catalogue, read the way the app reads it.
 *
 * build_programs.py validates the authored files, but nothing there proves the
 * result parses into the types the engine actually uses — a field named slightly
 * wrong would build cleanly and arrive empty. This is the other half of that.
 */
class ProgramAssetTest {
    @Serializable
    private data class ProgramCatalogAsset(val programs: List<ProgramAsset> = emptyList())

    @Serializable
    private data class ProgramAsset(
        val id: String,
        val level: String,
        val title: Map<String, String> = emptyMap(),
        val config: JsonElement = JsonObject(emptyMap()),
    )

    private val json = Json { ignoreUnknownKeys = true }

    // The shipped file, read from the module that owns it. Proving the engine's own
    // types come out of the very asset that ships is the point; a fixture here would
    // only prove the fixture.
    private val catalogue: ProgramCatalogAsset =
        json.decodeFromString(File("../data/src/androidMain/assets/programs.json").readText())

    private fun configOf(id: String): ProgramConfig {
        val program = catalogue.programs.first { it.id == id }
        return json.decodeFromString(program.config.toString())
    }

    @Test
    fun `the catalogue ships at least one program`() {
        assertTrue(catalogue.programs.isNotEmpty())
    }

    @Test
    fun `every program's configuration parses into the engine's own types`() {
        catalogue.programs.forEach { program ->
            val config = configOf(program.id)
            assertTrue(
                "${program.id} parsed to an empty configuration, so a field is named wrong",
                config.dailyPlan.activities.isNotEmpty(),
            )
        }
    }

    @Test
    fun `the A1 program says what it is meant to say`() {
        val program = catalogue.programs.first { it.id == "a1-essentials" }
        assertEquals("A1", program.level)
        assertEquals("Polish A1", program.title["en"])

        val config = configOf("a1-essentials")
        assertEquals(1000, config.goals.first { it.type == TargetType.VOCABULARY }.target)
        assertEquals(10, config.dailyPlan.newWords)
        assertEquals(20, config.dailyPlan.reviewWords)
        assertEquals(5, config.milestones.size)

        // Scope reaches the thousand words the goal asks for.
        val source = config.scope.include.single()
        assertEquals(ScopeSourceType.PRESET, source.type)
        assertEquals("top-1000", source.value)

        // Weights add up, which is what lets the progress figure mean anything.
        val weights = config.progress
        val total = weights.vocabulary + weights.milestones + weights.retention +
            weights.consistency + weights.studyTime + weights.accuracy
        assertEquals(100, total)
    }

    @Test
    fun `every activity names a training that exists`() {
        val known = setOf(
            "dictation", "dictation_puzzle", "true_or_false", "word_match", "pronunciation_check",
            "puzzle", "image_test", "memory_cards", "mix", "crossword",
        )
        catalogue.programs.forEach { program ->
            val config = configOf(program.id)
            val activities = config.dailyPlan.activities + config.dailyPlan.weekend?.activities.orEmpty()
            activities.forEach { activity ->
                assertTrue("${program.id}.${activity.id} has no trainings", activity.trainings.isNotEmpty())
                activity.trainings.forEach {
                    assertTrue("${program.id}.${activity.id} names unknown training $it", it in known)
                }
            }
        }
    }

    @Test
    fun `the weekend plan drops new words and leans on review`() {
        val weekend = configOf("a1-essentials").dailyPlan.weekend
        requireNotNull(weekend)
        assertEquals(0, weekend.newWords)
        assertTrue(weekend.reviewWords > 0)
        assertTrue(weekend.activities.any { it.type == ActivityType.REVIEW })
    }
}

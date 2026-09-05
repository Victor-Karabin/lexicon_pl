package com.lexicon.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val SEEDING_TIMEOUT_MS = 120_000L
private const val SETTLE_TIMEOUT_MS = 20_000L

/**
 * Walks the app the way a learner does, over the real database and the real catalogues.
 * The first launch seeds 2,562 words, so every wait is generous rather than tight.
 */
@RunWith(AndroidJUnit4::class)
class LexiconJourneyTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun waitForText(
        text: String,
        timeout: Long = SETTLE_TIMEOUT_MS,
    ) = rule.waitUntil(timeout) { rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }

    private fun openTab(name: String) {
        waitForText(name, SEEDING_TIMEOUT_MS)
        rule.onAllNodesWithText(name).onFirst().performClick()
        rule.waitForIdle()
    }

    @Test
    fun theAppSeedsAndLandsOnTheFiveTabs() {
        waitForText("Home", SEEDING_TIMEOUT_MS)

        listOf("Home", "Trainings", "Words", "Plan", "Settings").forEach {
            rule.onAllNodesWithText(it).onFirst().assertIsDisplayed()
        }
    }

    @Test
    fun theCatalogueIsThereAndAPresetCanBeStarred() {
        openTab("Words")
        waitForText("Top 100", SEEDING_TIMEOUT_MS)

        rule.onNodeWithText("Top 100").assertIsDisplayed()

        // The heart carries a content description rather than text, and says which way it goes.
        starTopHundredIfItIsNotAlready()

        assertTrue(
            rule.onAllNodesWithContentDescription("Remove every word from study set").fetchSemanticsNodes().isNotEmpty(),
        )
    }

    @Test
    fun searchingTheVocabularyNarrowsItToTheWordAsked() {
        openTab("Words")
        waitForText("Search words", SEEDING_TIMEOUT_MS)

        rule.onNodeWithText("Search words").performTextInput("kobieta")

        rule.waitUntil(SETTLE_TIMEOUT_MS) {
            rule.onAllNodesWithText("woman").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(rule.onAllNodesWithText("kobieta").fetchSemanticsNodes().isNotEmpty())
    }

    /** A lazy list only composes what is on screen, so each one has to be scrolled to. */
    @Test
    fun everyTrainingTheCatalogueOffersIsReachable() {
        openTab("Trainings")
        waitForText("Dictation", SEEDING_TIMEOUT_MS)

        listOf("Dictation", "Word Match", "True or False", "Crossword", "Word Card", "Mix").forEach { training ->
            rule.onNode(hasScrollAction()).performScrollToNode(hasText(training, substring = true))
            rule.onAllNodesWithText(training, substring = true).onFirst().assertIsDisplayed()
        }
    }

    /**
     * The whole point of the app in one journey: choose words, then train on them. Every
     * test here shares one database, so this one stars its own preset rather than relying
     * on whatever ran before it.
     */
    @Test
    fun wordsChosenInTheCatalogueAreWhatATrainingRuns() {
        openTab("Words")
        waitForText("Top 100", SEEDING_TIMEOUT_MS)
        starTopHundredIfItIsNotAlready()

        openTab("Trainings")
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Word Card", substring = true))
        rule.onAllNodesWithText("Word Card", substring = true).onFirst().performClick()

        rule.waitUntil(SEEDING_TIMEOUT_MS) {
            rule.onAllNodesWithText("Next").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText("Done").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun starTopHundredIfItIsNotAlready() {
        val unstarred = rule.onAllNodesWithContentDescription("Add every word to study set").fetchSemanticsNodes()
        if (unstarred.isEmpty()) return

        rule.onAllNodesWithContentDescription("Add every word to study set").onFirst().performClick()
        rule.waitUntil(SETTLE_TIMEOUT_MS) {
            rule.onAllNodesWithContentDescription("Remove every word from study set").fetchSemanticsNodes().isNotEmpty()
        }
    }
}

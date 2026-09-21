package com.lexicon.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
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
private const val WORDS_TO_SORT = 6
private const val STATUS_COUNT = 4

private const val TO_LEARN = "To learn. Tap to mark as favourite"
private const val RESET_QUEUE = "Start the queue again"

@RunWith(AndroidJUnit4::class)
class LexiconJourneyTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun shows(
        text: String,
        substring: Boolean = true,
    ): Boolean = rule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()

    private fun hasDescription(description: String): Boolean =
        rule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()

    private fun waitForText(
        text: String,
        timeout: Long = SETTLE_TIMEOUT_MS,
    ) = rule.waitUntil(timeout) { shows(text) }

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
    fun searchingTheVocabularyNarrowsItToTheWordAsked() {
        openTab("Words")
        waitForText("Search words", SEEDING_TIMEOUT_MS)

        rule.onNodeWithText("Search words").performTextInput("kobieta")

        rule.waitUntil(SETTLE_TIMEOUT_MS) { shows("woman", substring = false) }
        assertTrue(shows("kobieta", substring = false))
    }

    @Test
    fun aWordFoundBySearchCanBeMarkedToLearn() {
        openTab("Words")
        waitForText("Search words", SEEDING_TIMEOUT_MS)
        rule.onNodeWithText("Search words").performTextInput("kobieta")
        rule.waitUntil(SETTLE_TIMEOUT_MS) { shows("woman", substring = false) }

        repeat(STATUS_COUNT) {
            if (hasDescription(TO_LEARN)) return@repeat
            val next = rule.onAllNodesWithContentDescription("Tap to", substring = true).onFirst()
            next.performClick()
            rule.waitForIdle()
        }

        assertTrue(hasDescription(TO_LEARN))
    }

    @Test
    fun everyTrainingTheCatalogueOffersIsReachable() {
        openTab("Trainings")
        waitForText("Dictation", SEEDING_TIMEOUT_MS)

        listOf("Dictation", "Word Match", "True or False", "Crossword", "Word Card", "Mix").forEach { training ->
            rule.onNode(hasScrollAction()).performScrollToNode(hasText(training, substring = true))
            rule.onAllNodesWithText(training, substring = true).onFirst().assertIsDisplayed()
        }
    }

    @Test
    fun wordsSortedInReviewAreWhatATrainingRuns() {
        sortSomeWordsToLearn()

        openTab("Trainings")
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Word Card", substring = true))
        rule.onAllNodesWithText("Word Card", substring = true).onFirst().performClick()

        rule.waitUntil(SEEDING_TIMEOUT_MS) { shows("Next", substring = false) || shows("Done", substring = false) }
    }

    @Test
    fun theVocabularyCourseRunsATrainingThatCanBeReset() {
        sortSomeWordsToLearn()
        openTab("Home")
        waitForText("Vocabulary course", SEEDING_TIMEOUT_MS)

        if (shows("Meet ")) {
            rule.onAllNodesWithText("Meet ", substring = true).onFirst().performClick()
            rule.waitUntil(SETTLE_TIMEOUT_MS) { shows("Next", substring = false) || shows("Start training", substring = false) }
            while (!shows("Start training", substring = false)) {
                rule.onAllNodesWithText("Next", substring = false).onFirst().performClick()
                rule.waitForIdle()
            }
            rule.onAllNodesWithText("Start training", substring = false).onFirst().performClick()
        } else {
            rule.onAllNodesWithText("Continue", substring = false).onFirst().performClick()
        }

        rule.waitUntil(SEEDING_TIMEOUT_MS) { hasDescription(RESET_QUEUE) }
    }

    private fun waitingLine(): String =
        rule
            .onAllNodesWithText("still to sort", substring = true)
            .fetchSemanticsNodes()
            .firstOrNull()
            ?.config
            ?.getOrNull(SemanticsProperties.Text)
            ?.joinToString()
            .orEmpty()

    private fun sortSomeWordsToLearn() {
        openTab("Home")
        waitForText("Review words", SEEDING_TIMEOUT_MS)
        rule.onAllNodesWithText("Review words").onFirst().performClick()
        rule.waitUntil(SETTLE_TIMEOUT_MS) { shows("still to sort") }

        repeat(WORDS_TO_SORT) {
            val before = waitingLine()
            rule.onAllNodesWithText("To learn", substring = false).onFirst().performClick()
            rule.waitUntil(SETTLE_TIMEOUT_MS) { waitingLine() != before }
        }
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }
}

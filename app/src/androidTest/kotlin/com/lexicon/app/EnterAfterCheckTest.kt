package com.lexicon.app

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.input.ImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lexicon.interactors.course.TranscribeItem
import com.lexicon.presentation.R
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.TrainingActionRow
import com.lexicon.presentation.course.TranscribeRow
import com.lexicon.presentation.theme.LexiconTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
@RunWith(AndroidJUnit4::class)
class EnterAfterCheckTest {
    @get:Rule
    val rule = createComposeRule()

    private val next = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.action_next)

    private var checks by mutableIntStateOf(0)
    private var nexts by mutableIntStateOf(0)
    private var closes by mutableIntStateOf(0)
    private var answered by mutableStateOf(false)

    private fun showAnswerWithKeyboard() {
        rule.setContent {
            val inputMode = LocalInputModeManager.current
            val field = remember { FocusRequester() }
            var typed by remember { mutableStateOf("") }
            val check = {
                checks++
                answered = true
            }
            LaunchedEffect(Unit) {
                inputMode.requestInputMode(InputMode.Keyboard)
                field.requestFocus()
            }
            LexiconTheme {
                Column {
                    TextButton(onClick = { closes++ }) { Text("Close") }
                    TranscribeRow(
                        item = TranscribeItem("", "jedno"),
                        value = typed,
                        isCorrect = if (answered) true else null,
                        answerState = if (answered) AnswerState.Correct else AnswerState.Unanswered,
                        onValueChanged = { typed = it },
                        imeAction = ImeAction.Done,
                        onDone = check,
                        focusRequester = field,
                        reservesInfoSlot = false,
                    )
                    TrainingActionRow(
                        onCheck = check,
                        onNext = { nexts++ },
                        awaitingNext = answered,
                        checkEnabled = typed.isNotBlank(),
                    )
                }
            }
        }
    }

    @Test
    fun enterOnTheAnswerChecksItAndHandsTheKeyboardFocusToNext() {
        showAnswerWithKeyboard()
        rule.onNode(hasSetTextAction()).performTextInput("jedno")

        rule.onNode(hasSetTextAction()).performKeyInput { keyDown(Key.Enter) }

        rule.onNodeWithText(next).assertIsFocused()
        assertEquals(1, checks)
    }

    @Test
    fun theReleaseOfTheEnterThatCheckedDoesNotAlsoPressNext() {
        showAnswerWithKeyboard()
        rule.onNode(hasSetTextAction()).performTextInput("jedno")
        rule.onNode(hasSetTextAction()).performKeyInput { keyDown(Key.Enter) }

        rule.onNodeWithText(next).performKeyInput { keyUp(Key.Enter) }

        assertEquals(0, nexts)
    }

    @Test
    fun aFullEnterPressOnNextMovesOn() {
        showAnswerWithKeyboard()
        rule.onNode(hasSetTextAction()).performTextInput("jedno")
        rule.onNode(hasSetTextAction()).performKeyInput { keyDown(Key.Enter) }
        rule.onNodeWithText(next).performKeyInput { keyUp(Key.Enter) }

        rule.onNodeWithText(next).performKeyInput { pressKey(Key.Enter) }

        assertEquals(1, nexts)
        assertEquals(1, checks)
        assertEquals(0, closes)
    }
}

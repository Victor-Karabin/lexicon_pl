package com.lexicon.baselineprofile

import android.util.Log
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE = "com.lexicon"
private const val TIMEOUT_MS = 10_000L
private const val FLINGS = 4
private const val GESTURE_MARGIN_FRACTION = 5
private const val MAX_FLINGS_BACK = 20
private const val EDIT_TEXT = "android.widget.EditText"
private const val ATTEMPTS = 3
private const val TAG = "BaselineProfile"

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startupAndLongLists() =
        rule.collect(packageName = PACKAGE, maxIterations = 3, stableIterations = 2, includeInStartupProfile = true) {
            killProcess()
            startActivityAndWait()

            tap("Words")
            tap("Top 1000")
            device.wait(Until.hasObject(By.text("1,000 words")), TIMEOUT_MS)
            flingLongestList()
            device.pressBack()
            device.wait(Until.hasObject(By.text("Top 100")), TIMEOUT_MS)
            flingLongestList()

            device.wait(Until.findObject(By.clazz(EDIT_TEXT)), TIMEOUT_MS).text = "ka"
            device.waitForIdle()
            flingLongestList()
            device.wait(Until.findObject(By.clazz(EDIT_TEXT)), TIMEOUT_MS).text = ""

            tap("Plan")
            tap("Polish for Beginners 1")
            tap("Lesson 1")
            device.wait(Until.hasObject(By.text("New words")), TIMEOUT_MS)
            flingLongestList()
            device.pressBack()
            device.pressBack()

            tap("Verb Conjugation")
            device.wait(Until.hasObject(By.text("Search verb")), TIMEOUT_MS)
            flingLongestList()
            device.pressBack()
        }

    private fun MacrobenchmarkScope.tap(text: String) {
        device.wait(Until.findObject(By.text(text)), TIMEOUT_MS).click()
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.longestList(): UiObject2 {
        device.wait(Until.hasObject(By.scrollable(true)), TIMEOUT_MS)
        return device.findObjects(By.scrollable(true)).maxBy { runCatching { it.visibleBounds.height() }.getOrDefault(0) }
    }

    private fun MacrobenchmarkScope.fling(direction: Direction): Boolean {
        repeat(ATTEMPTS) {
            try {
                val list = longestList()
                list.setGestureMargin(device.displayWidth / GESTURE_MARGIN_FRACTION)
                val moved = list.fling(direction)
                device.waitForIdle()
                return moved
            } catch (stale: StaleObjectException) {
                Log.w(TAG, "The list was redrawn before the fling; trying again", stale)
                device.waitForIdle()
            }
        }
        return false
    }

    private fun MacrobenchmarkScope.flingLongestList() {
        repeat(FLINGS) { fling(Direction.DOWN) }
        var flungBack = 0
        while (flungBack < MAX_FLINGS_BACK && fling(Direction.UP)) flungBack++
    }
}

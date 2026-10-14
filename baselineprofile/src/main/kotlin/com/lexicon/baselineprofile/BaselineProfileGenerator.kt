package com.lexicon.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE = "com.lexicon"
private const val TIMEOUT_MS = 10_000L
private const val FLINGS = 6

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startupAndWordListScrolling() =
        rule.collect(packageName = PACKAGE, maxIterations = 3, stableIterations = 2, includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()

            device.wait(Until.findObject(By.text("Words")), TIMEOUT_MS).click()
            val presets = device.wait(Until.findObject(By.scrollable(true)), TIMEOUT_MS)
            presets.scrollUntil(Direction.DOWN, Until.findObject(By.text("Top 2000")))?.click()
                ?: device.findObject(By.text("Top 2000")).click()

            device.wait(Until.hasObject(By.text("2,000 words")), TIMEOUT_MS)
            val words = device.wait(Until.findObject(By.scrollable(true)), TIMEOUT_MS)
            repeat(FLINGS) {
                words.fling(Direction.DOWN)
                device.waitForIdle()
            }
        }
}

package com.lexicon.presentation.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log

private const val TAG = "PolishSetup"
private const val TTS_SETTINGS = "com.android.settings.TTS_SETTINGS"

data class PolishSetup(
    val needsKeyboard: Boolean = false,
    val needsVoice: Boolean = false,
) {
    val isComplete: Boolean get() = !needsKeyboard && !needsVoice
}

fun Context.openKeyboardSettings() = openFirst(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS), Intent(Settings.ACTION_SETTINGS))

fun Context.openVoiceInstall() = openFirst(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA), Intent(TTS_SETTINGS))

private fun Context.openFirst(vararg intents: Intent) {
    for (intent in intents) {
        try {
            startActivity(intent)
            return
        } catch (missing: ActivityNotFoundException) {
            Log.w(TAG, "No screen handles ${intent.action}; trying the next one", missing)
        }
    }
}

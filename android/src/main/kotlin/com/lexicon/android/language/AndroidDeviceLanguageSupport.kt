package com.lexicon.android.language

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.inputmethod.InputMethodManager
import com.lexicon.boundary.DeviceLanguageSupport
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

private const val TAG = "DeviceLanguageSupport"
private const val POLISH = "pl"
private const val VOICE_CHECK_TIMEOUT_MS = 5_000L

class AndroidDeviceLanguageSupport(
    private val context: Context,
) : DeviceLanguageSupport {
    override suspend fun hasPolishKeyboard(): Boolean {
        val inputMethods = context.getSystemService(InputMethodManager::class.java) ?: return true
        return inputMethods.enabledInputMethodList.any { method ->
            inputMethods.getEnabledInputMethodSubtypeList(method, true).any { subtype ->
                Locale.forLanguageTag(subtype.languageTag).language == POLISH
            }
        }
    }

    override suspend fun hasPolishVoice(): Boolean {
        val available = withTimeoutOrNull(VOICE_CHECK_TIMEOUT_MS) { polishVoiceStatus() }
        if (available == null) Log.w(TAG, "The text-to-speech engine did not answer in time")
        return available ?: true
    }

    private suspend fun polishVoiceStatus(): Boolean =
        suspendCancellableCoroutine { continuation ->
            lateinit var engine: TextToSpeech
            engine = TextToSpeech(context) { status ->
                val available = status == TextToSpeech.SUCCESS &&
                    engine.isLanguageAvailable(Locale(POLISH)) >= TextToSpeech.LANG_AVAILABLE
                if (status != TextToSpeech.SUCCESS) Log.w(TAG, "The text-to-speech engine failed to start (status=$status)")
                engine.shutdown()
                if (continuation.isActive) continuation.resume(available)
            }
            continuation.invokeOnCancellation { engine.shutdown() }
        }
}

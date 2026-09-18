package com.stepwise.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Saida de audio por Android TextToSpeech.
 * No evento, a rota pode sair pelo celular ou pelos oculos pareados via Bluetooth.
 */
class TtsManager(context: Context) {

    data class Callbacks(
        val onStart: () -> Unit = {},
        val onDone: () -> Unit = {},
        val onError: () -> Unit = {},
    )

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingSpeech: PendingSpeech? = null
    private val callbacks = ConcurrentHashMap<String, Callbacks>()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                configureEngine()
                pendingSpeech?.let { speakNow(it.text, it.callbacks) }
                pendingSpeech = null
            } else {
                pendingSpeech?.callbacks?.onError?.invoke()
                pendingSpeech = null
            }
        }
    }

    fun speak(text: String, callbacks: Callbacks = Callbacks()) {
        if (text.isBlank()) return
        if (!ready) {
            pendingSpeech = PendingSpeech(text, callbacks)
            return
        }
        speakNow(text, callbacks)
    }

    fun shutdown() {
        pendingSpeech = null
        callbacks.clear()
        ready = false
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun configureEngine() {
        val brazilianPortuguese = Locale.forLanguageTag("pt-BR")
        val result = tts?.setLanguage(brazilianPortuguese)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.language = Locale.getDefault()
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                callbacks[utteranceId]?.onStart?.invoke()
            }

            override fun onDone(utteranceId: String) {
                callbacks.remove(utteranceId)?.onDone?.invoke()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                callbacks.remove(utteranceId)?.onError?.invoke()
            }

            override fun onError(utteranceId: String, errorCode: Int) {
                callbacks.remove(utteranceId)?.onError?.invoke()
            }
        })
    }

    private fun speakNow(text: String, callbacks: Callbacks) {
        val utteranceId = "stepwise-${System.nanoTime()}"
        this.callbacks[utteranceId] = callbacks
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            this.callbacks.remove(utteranceId)?.onError?.invoke()
        }
    }

    private data class PendingSpeech(
        val text: String,
        val callbacks: Callbacks,
    )
}

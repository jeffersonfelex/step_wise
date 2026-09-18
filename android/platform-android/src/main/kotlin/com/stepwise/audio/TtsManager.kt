package com.stepwise.audio

import android.content.Context
import android.speech.tts.TextToSpeech

/**
 * Saída de áudio (TTS no diagrama): Android TextToSpeech roteado por
 * Bluetooth para os alto-falantes open-ear dos Ray-Ban Meta.
 * Compartilhado pelos modos; pode falar instruções prontas ou texto do tutor.
 * Áudio nativo em streaming de um provedor precisa de uma saída PCM separada.
 */
class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { /* TODO: tratar status de inicialização */ }
    }

    fun speak(text: String) {
        // TODO: tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun shutdown() {
        tts?.shutdown()
    }
}

package com.stepwise.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Saída de áudio (TTS no diagrama): Android TextToSpeech roteado por
 * Bluetooth para os alto-falantes open-ear dos Ray-Ban Meta.
 * Compartilhado pelos modos; pode falar instruções prontas ou texto do tutor.
 * Áudio nativo em streaming de um provedor precisa de uma saída PCM separada.
 *
 * Fala em português (pt-BR), já que é o idioma de todo o conteúdo do app
 * (instruções dos procedimentos, feedback da IA). Se o motor TTS do
 * aparelho não tiver o idioma instalado, cai para o idioma padrão do
 * dispositivo em vez de falhar silenciosamente.
 */
class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false
    // Fala pedida antes do TTS terminar de inicializar (ex.: "Sessão iniciada"
    // logo no onCreate) não pode ser perdida - guarda a última e fala assim
    // que o motor ficar pronto.
    private var pendingText: String? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                val result = tts?.setLanguage(Locale.forLanguageTag("pt-BR"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "pt-BR indisponível no motor TTS deste aparelho; usando idioma padrão.")
                }
                pendingText?.let { speak(it) }
                pendingText = null
            } else {
                Log.e(TAG, "Falha ao inicializar o TextToSpeech (status=$status). Feedback ficará mudo.")
            }
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        val engine = tts
        if (!ready || engine == null) {
            pendingText = text
            return
        }
        // QUEUE_FLUSH: fala nova interrompe a anterior - instruções e feedback
        // de verificação são curtos e a mais recente é sempre a relevante.
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    private companion object {
        const val TAG = "TtsManager"
    }
}

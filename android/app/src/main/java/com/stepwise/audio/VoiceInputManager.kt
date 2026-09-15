package com.stepwise.audio

import com.stepwise.session.VoiceCommand

/**
 * Entrada de voz (VOICE no diagrama): áudio via Bluetooth HFP dos
 * Ray-Ban Meta, reconhecido com Android SpeechRecognizer.
 *
 * Regra de bateria: HFP/ASR fica ativo apenas em janelas curtas, quando se
 * espera um comando — não é reconhecimento contínuo.
 */
class VoiceInputManager(
    private val onCommand: (VoiceCommand) -> Unit
) {

    fun startListeningWindow() {
        // TODO: abrir janela curta de ASR via SpeechRecognizer sobre HFP.
        // TODO: mapear frases reconhecidas ("verificar", "ajuda", "repetir") -> VoiceCommand.
    }

    fun stopListeningWindow() {
        // TODO: encerrar ASR e liberar o canal HFP.
    }
}

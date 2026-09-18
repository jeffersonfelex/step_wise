package com.stepwise.audio

import com.stepwise.session.VoiceCommand
import java.text.Normalizer

/**
 * Entrada de voz em janelas curtas.
 *
 * A integracao com o reconhecedor offline real ainda entra por tras de
 * handleTranscript(). Assim conseguimos testar o vocabulario e o controlador
 * agora, sem prender o app a um provedor de ASR durante a preparacao.
 */
class VoiceInputManager(
    private val onCommand: (VoiceCommand) -> Unit,
) {
    private var listening = false

    fun startListeningWindow() {
        listening = true
    }

    fun stopListeningWindow() {
        listening = false
    }

    fun isListening(): Boolean = listening

    fun handleTranscript(transcript: String): VoiceCommand {
        val command = parseCommand(transcript)
        if (listening) {
            onCommand(command)
            stopListeningWindow()
        }
        return command
    }

    companion object {
        fun parseCommand(transcript: String): VoiceCommand {
            val normalized = transcript
                .lowercase()
                .withoutAccents()
                .replace(Regex("[^a-z0-9 ]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            if (normalized.isBlank()) return VoiceCommand.UNKNOWN

            val words = normalized.split(' ').toSet()
            return when {
                "verificar" in words || "confere" in words || "conferir" in words -> VoiceCommand.VERIFY
                "ajuda" in words || "ajudar" in words -> VoiceCommand.HELP
                "repetir" in words || "repete" in words || "novamente" in words -> VoiceCommand.REPEAT
                else -> VoiceCommand.UNKNOWN
            }
        }

        private fun String.withoutAccents(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
    }
}

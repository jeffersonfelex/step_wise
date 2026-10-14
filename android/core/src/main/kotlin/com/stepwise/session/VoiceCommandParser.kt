package com.stepwise.session

import java.text.Normalizer

/**
 * Converte uma transcrição curta em comando. Tolera acentos, pontuação e
 * variações comuns ("confere", "me ajudar", "repete"); devolve null para fala
 * sem comando, que não deve capturar imagem nem alterar o estado da sessão.
 */
object VoiceCommandParser {

    /** Palavras aceitas; o reconhecedor offline pode usá-las como gramática restrita. */
    val vocabulary: List<String> = listOf(
        "verificar", "conferir", "confere",
        "ajuda", "ajudar",
        "repetir", "repete", "novamente",
    )

    fun parse(transcript: String): VoiceCommand? {
        val words = Normalizer.normalize(transcript.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(' ')
            .filter { it.isNotEmpty() }
            .toSet()
        return when {
            "verificar" in words || "confere" in words || "conferir" in words -> VoiceCommand.VERIFY
            "ajuda" in words || "ajudar" in words -> VoiceCommand.HELP
            "repetir" in words || "repete" in words || "novamente" in words -> VoiceCommand.REPEAT
            else -> null
        }
    }
}

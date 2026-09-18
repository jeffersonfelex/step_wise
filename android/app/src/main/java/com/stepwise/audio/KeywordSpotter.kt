package com.stepwise.audio

import com.stepwise.session.VoiceCommand

/**
 * Reconhecedor dos 3 comandos ("verificar", "ajuda", "repetir").
 *
 * Decisão revisada (feedback da mentoria, 15/09): o Android
 * SpeechRecognizer padrão pode enviar o áudio para servidores do
 * Google, o que contradiz o checkpoint de privacidade ("sem envio à
 * nuvem"). Como só existem 3 comandos, um detector de palavras-chave
 * (KWS) offline é determinístico, privado e funciona sem rede.
 *
 * Implementações candidatas (escolher UMA a tempo do evento):
 *  - sherpa-onnx KWS
 *  - Vosk com gramática restrita às 3 palavras
 *  - Porcupine (wake word engine da Picovoice)
 *  - createOnDeviceSpeechRecognizer (Android 12+) como alternativa que
 *    já vem no framework, se disponível no aparelho-alvo.
 *
 * Direcionar o SpeechRecognizer ao microfone Bluetooth é instável; ler
 * o áudio direto da rota SCO via AudioRecord dá controle real da fonte
 * (garantindo que o comando "verificar" vem dos óculos, não do
 * microfone do celular).
 */
interface KeywordSpotter {

    /** Inicia a escuta contínua (ver VoiceInputManager: HFP fica ligado por toda a sessão). */
    fun start(onCommand: (VoiceCommand) -> Unit)

    fun stop()
}

/**
 * TODO: integrar com o backend de KWS escolhido (sherpa-onnx / Vosk /
 * Porcupine). Deve ler PCM 16kHz/8kHz mono direto da rota SCO
 * (AudioRecord), nunca enviar áudio para fora do dispositivo.
 */
class OfflineKeywordSpotter : KeywordSpotter {

    override fun start(onCommand: (VoiceCommand) -> Unit) {
        // TODO: abrir AudioRecord na rota SCO e alimentar o modelo de KWS offline.
        // TODO: mapear detecções de palavra-chave -> VoiceCommand (VERIFY/HELP/REPEAT).
    }

    override fun stop() {
        // TODO: liberar AudioRecord / modelo.
    }
}

/**
 * Alternativa on-device do próprio Android (API 31+), sem envio à
 * nuvem quando o reconhecedor on-device está disponível no aparelho.
 * Usar apenas se validado no aparelho-alvo antes do evento.
 */
class OnDeviceSpeechRecognizerSpotter : KeywordSpotter {

    override fun start(onCommand: (VoiceCommand) -> Unit) {
        // TODO: SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        // TODO: confirmar (isOnDeviceRecognitionAvailable) antes de usar; se
        // indisponível, cair para OfflineKeywordSpotter.
    }

    override fun stop() {
        // TODO: recognizer?.destroy()
    }
}

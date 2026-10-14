package com.stepwise.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.stepwise.session.VoiceCommand
import com.stepwise.session.VoiceCommandParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Recognizer

/**
 * Reconhecedor dos 3 comandos ("verificar", "ajuda", "repetir").
 *
 * Decisão revisada (feedback da mentoria, 15/09): o Android
 * SpeechRecognizer padrão pode enviar o áudio para servidores do
 * Google, o que contradiz o checkpoint de privacidade ("sem envio à
 * nuvem"). Como só existem 3 comandos, um detector de palavras-chave
 * (KWS) offline é determinístico, privado e funciona sem rede.
 *
 * Implementação escolhida: Vosk com gramática restrita ao vocabulário dos 3 comandos
 * (ver OfflineKeywordSpotter). Alternativas descartadas por ora:
 * sherpa-onnx KWS, Porcupine, createOnDeviceSpeechRecognizer (mantido
 * abaixo como fallback opcional em aparelhos Android 12+).
 *
 * Direcionar o SpeechRecognizer ao microfone Bluetooth é instável; ler
 * o áudio direto da rota SCO via AudioRecord dá controle real da fonte
 * (garantindo que o comando "verificar" vem dos óculos, não do
 * microfone do celular) — ver VoiceInputManager, que abre a rota SCO
 * antes de chamar start() aqui.
 *
 * IMPORTANTE (achado em produção, 18/09/2026, hackathon - comandos de
 * voz não reconhecidos com óculos conectados via Bluetooth): até esta
 * revisão, TODA falha aqui (modelo Vosk ausente, AudioRecord que não
 * inicializa, buffer inválido) só ia para o Logcat - completamente
 * silenciosa para quem está testando sem um cabo USB/Android Studio
 * abertos. Agora [onError] propaga uma mensagem legível para a Activity
 * mostrar na tela e falar por TTS, para dar pelo menos um diagnóstico
 * na hora em vez de "não fez nada".
 */
interface KeywordSpotter {

    /** Inicia a escuta contínua (ver VoiceInputManager: HFP fica ligado por toda a sessão). */
    fun start(onCommand: (VoiceCommand) -> Unit, onError: (String) -> Unit = {})

    fun stop()
}

/**
 * KWS offline via Vosk. Lê PCM 16-bit mono direto de um AudioRecord na
 * rota SCO (aberta por VoiceInputManager) e alimenta um Recognizer com
 * gramática restrita ao vocabulário dos comandos — nunca envia áudio para
 * fora do dispositivo.
 *
 * O modelo (assets/model, ~50MB) é baixado ANTES do build por
 * scripts/download_vosk_model.sh ou pela tarefa Gradle downloadVoskModel
 * (:platform-android) — não há download em tempo de execução.
 */
class OfflineKeywordSpotter(private val context: Context) : KeywordSpotter {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var listenJob: Job? = null

    override fun start(onCommand: (VoiceCommand) -> Unit, onError: (String) -> Unit) {
        if (listenJob?.isActive == true) return
        listenJob = scope.launch {
            val model = VoskModelProvider.awaitModel(context)
            if (model == null) {
                Log.e(TAG, "Modelo Vosk indisponível; comandos de voz não serão reconhecidos.")
                onError("Modelo de reconhecimento de voz não encontrado. Comandos de voz indisponíveis; use os botões na tela.")
                return@launch
            }

            val recognizer = Recognizer(model, SAMPLE_RATE, GRAMMAR)
            val minBuffer = AudioRecord.getMinBufferSize(
                SAMPLE_RATE.toInt(), AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuffer <= 0) {
                Log.e(TAG, "AudioRecord.getMinBufferSize retornou inválido ($minBuffer).")
                onError("Não consegui configurar o microfone. Comandos de voz indisponíveis; use os botões na tela.")
                recognizer.close()
                return@launch
            }

            @Suppress("MissingPermission") // RECORD_AUDIO é checada pela Activity antes de startSession().
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE.toInt(),
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuffer * 2
            )

            // Achado em produção (18/09/2026): em alguns aparelhos/rotas (ex.:
            // logo após o SCO "conectar" mas antes da rota estabilizar de
            // verdade), o AudioRecord fica em STATE_UNINITIALIZED e nunca lê
            // nada - sem esse check, o app fica escutando silêncio para
            // sempre, sem erro nenhum.
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord não inicializou (state=${record.state}). Rota de áudio provavelmente não é a dos óculos ainda.")
                onError("Não consegui acessar o microfone dos óculos. Comandos de voz indisponíveis; use os botões na tela.")
                record.release()
                recognizer.close()
                return@launch
            }

            val buffer = ByteArray(minBuffer)
            // Só esta coroutine para e libera o gravador: liberar em stop() enquanto
            // read() ainda roda faria o stop() abaixo lançar IllegalStateException.
            try {
                record.startRecording()
                if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    Log.e(TAG, "AudioRecord.startRecording() não entrou em RECORDSTATE_RECORDING.")
                    onError("O microfone não começou a gravar. Comandos de voz indisponíveis; use os botões na tela.")
                    return@launch
                }
                while (isActive) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read < 0) {
                        Log.e(TAG, "AudioRecord.read() falhou ($read).")
                        onError("O microfone parou de responder. Comandos de voz indisponíveis; use os botões na tela.")
                        break
                    }
                    if (read > 0 && recognizer.acceptWaveForm(buffer, read)) {
                        parseCommand(recognizer.result)?.let { onCommand(it) }
                    }
                }
            } finally {
                runCatching { record.stop() }
                record.release()
                recognizer.close()
            }
        }
    }

    override fun stop() {
        // A leitura bloqueia no máximo um buffer (~poucas dezenas de ms) antes de ver o cancelamento.
        listenJob?.cancel()
        listenJob = null
    }

    private fun parseCommand(resultJson: String): VoiceCommand? {
        val text = runCatching { JSONObject(resultJson).optString("text", "") }.getOrDefault("")
        return VoiceCommandParser.parse(text)
    }

    companion object {
        private const val TAG = "OfflineKeywordSpotter"
        private const val SAMPLE_RATE = 16_000f

        // Gramática restrita: só o vocabulário dos comandos (mais "[unk]" para
        // tudo que não bate) é candidato ao resultado do Vosk.
        private val GRAMMAR = (VoiceCommandParser.vocabulary + "[unk]")
            .joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
    }
}

/**
 * Alternativa on-device do próprio Android (API 31+), sem envio à
 * nuvem quando o reconhecedor on-device está disponível no aparelho.
 * Usar apenas se validado no aparelho-alvo antes do evento.
 */
class OnDeviceSpeechRecognizerSpotter : KeywordSpotter {

    override fun start(onCommand: (VoiceCommand) -> Unit, onError: (String) -> Unit) {
        // TODO: SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        // TODO: confirmar (isOnDeviceRecognitionAvailable) antes de usar; se
        // indisponível, cair para OfflineKeywordSpotter.
    }

    override fun stop() {
        // TODO: recognizer?.destroy()
    }
}

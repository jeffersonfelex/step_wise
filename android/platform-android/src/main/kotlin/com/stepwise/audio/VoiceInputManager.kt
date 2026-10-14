package com.stepwise.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.stepwise.session.VoiceCommand

/**
 * Entrada de voz (VOICE no diagrama): áudio via Bluetooth HFP dos
 * Ray-Ban Meta, reconhecido por um KeywordSpotter offline.
 *
 * Decisão revisada (feedback da mentoria, 15/09): manter o HFP ligado
 * durante toda a sessão de montagem, em vez de abrir/fechar em janelas
 * curtas por verificação. Motivo: HFP e A2DP são mutuamente exclusivos e
 * cada troca de rota custa ~2s de estabilização — a TTS que tocar durante
 * a transição pode sair cortada ou pelo alto-falante do celular. Como o
 * aprendiz leva minutos por etapa, a "janela" antiga já ficava aberta a
 * maior parte do tempo mesmo assim. TTS a 8 kHz mono é suficiente para
 * instruções curtas mesmo com o HFP sempre ativo.
 *
 * IMPORTANTE: configurar/abrir o HFP *antes* de abrir o stream de câmera
 * do Meta DAT (ver MetaDatSessionManager) — na ordem inversa o áudio pode
 * falhar sem erro. Por isso startSession() só chama keywordSpotter.start()
 * depois que o Android confirma SCO_AUDIO_STATE_CONNECTED — nunca antes.
 *
 * IMPORTANTE (achado em produção, 18/09/2026, hackathon - comandos de voz
 * "verificar"/"ajuda"/"repetir" não reconhecidos mesmo com os óculos
 * conectados por Bluetooth): até esta revisão, se SCO_AUDIO_STATE_CONNECTED
 * nunca chegasse (observado em alguns aparelhos, ex. Xiaomi + Ray-Ban Meta,
 * mesmo com o Bluetooth "conectado" ao nível do sistema operacional), o
 * KeywordSpotter nunca era iniciado e nada era mostrado ao aluno — silêncio
 * total, sem qualquer diagnóstico. Agora [onStatus] repassa qualquer falha
 * (SCO não conectou a tempo, SCO desconectou, erro de SCO, ou uma falha
 * vinda do próprio KeywordSpotter — ver KeywordSpotter.onError) para a
 * Activity mostrar na tela e falar por TTS; e, se o SCO não conectar a
 * tempo, a escuta começa pelo microfone do celular em vez de ficar muda.
 *
 * [onCommand] e [onStatus] sempre chegam na thread principal.
 */
class VoiceInputManager(
    private val context: Context,
    private val keywordSpotter: KeywordSpotter = OfflineKeywordSpotter(context),
    private val onCommand: (VoiceCommand) -> Unit,
    private val onStatus: (String) -> Unit = {}
) {

    private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var listening = false
    private var scoReceiver: BroadcastReceiver? = null
    private var scoConnected = false

    private val scoTimeoutRunnable = Runnable {
        if (!scoConnected && listening) {
            Log.e(TAG, "SCO não conectou em ${SCO_TIMEOUT_MS}ms; ouvindo pelo microfone do celular.")
            notifyStatus(
                "Não consegui conectar o áudio dos óculos. Os comandos de voz usam o microfone do celular; " +
                    "confira se os óculos estão pareados e conectados por Bluetooth."
            )
            startSpotter()
        }
    }

    // O KeywordSpotter chama de uma thread de background: a UI só recebe na thread principal.
    private fun startSpotter() = keywordSpotter.start(
        onCommand = { command -> mainHandler.post { if (listening) onCommand(command) } },
        onError = { message -> notifyStatus(message) },
    )

    private fun notifyStatus(message: String) {
        mainHandler.post { if (listening) onStatus(message) }
    }

    /** Abre a rota HFP e liga a escuta contínua para toda a sessão de montagem. */
    fun startSession() {
        if (listening) return
        listening = true
        scoConnected = false
        openHfpRoute()
    }

    /** Encerra a escuta e libera o canal HFP ao final da sessão. */
    fun stopSession() {
        if (!listening) return
        listening = false
        mainHandler.removeCallbacks(scoTimeoutRunnable)
        keywordSpotter.stop()
        closeHfpRoute()
    }

    private fun openHfpRoute() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                when (intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR)) {
                    AudioManager.SCO_AUDIO_STATE_CONNECTED -> {
                        Log.i(TAG, "HFP conectado; iniciando KeywordSpotter.")
                        scoConnected = true
                        mainHandler.removeCallbacks(scoTimeoutRunnable)
                        startSpotter()
                    }
                    AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> {
                        Log.w(TAG, "HFP desconectado durante a sessão.")
                        val wasConnected = scoConnected
                        scoConnected = false
                        if (listening && wasConnected) {
                            notifyStatus("O áudio dos óculos foi desconectado. Comandos de voz indisponíveis; use os botões na tela.")
                        }
                        // TODO: reconectar (ver LinkStateManager) se a sessão ainda estiver ativa.
                    }
                    AudioManager.SCO_AUDIO_STATE_ERROR -> {
                        Log.e(TAG, "Erro ao abrir a rota SCO (HFP). Voz indisponível nesta sessão.")
                        notifyStatus("Não consegui abrir o áudio dos óculos. Comandos de voz indisponíveis; use os botões na tela.")
                    }
                }
            }
        }
        scoReceiver = receiver
        context.registerReceiver(receiver, IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED))

        // Ordem importa: modo de comunicação + startBluetoothSco() ANTES de
        // qualquer stream de câmera (ver MetaDatSessionManager).
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isBluetoothScoOn = true
        audioManager.startBluetoothSco()

        // Achado em produção (18/09/2026): em alguns aparelhos o
        // SCO_AUDIO_STATE_CONNECTED pode nunca chegar mesmo com o
        // Bluetooth "conectado" no nível do SO — sem este timeout o app
        // ficava esperando para sempre, sem qualquer aviso ao aluno.
        mainHandler.postDelayed(scoTimeoutRunnable, SCO_TIMEOUT_MS)
    }

    private fun closeHfpRoute() {
        audioManager.isBluetoothScoOn = false
        audioManager.stopBluetoothSco()
        audioManager.mode = AudioManager.MODE_NORMAL
        scoReceiver?.let { runCatching { context.unregisterReceiver(it) } }
        scoReceiver = null
    }

    companion object {
        private const val TAG = "VoiceInputManager"
        private const val SCO_TIMEOUT_MS = 6_000L
    }
}

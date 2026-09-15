package com.stepwise.audio

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
 * falhar sem erro.
 */
class VoiceInputManager(
    private val keywordSpotter: KeywordSpotter = OfflineKeywordSpotter(),
    private val onCommand: (VoiceCommand) -> Unit
) {

    private var listening = false

    /** Abre a rota HFP e liga a escuta contínua para toda a sessão de montagem. */
    fun startSession() {
        if (listening) return
        // TODO: configurar/abrir HFP antes do stream de câmera.
        keywordSpotter.start(onCommand)
        listening = true
    }

    /** Encerra a escuta e libera o canal HFP ao final da sessão. */
    fun stopSession() {
        if (!listening) return
        keywordSpotter.stop()
        listening = false
    }
}

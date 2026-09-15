package com.stepwise.glasses

/**
 * Encapsula a DeviceSession do Meta DAT v0.9 (Ray-Ban Meta Gen 2).
 *
 * Decisão revisada (feedback da mentoria, 15/09): preferir o melhor frame
 * do STREAM já aberto em vez de uma chamada isolada a capturePhoto().
 * Dois motivos:
 *  1. Há relato de bug do SDK em que capturePhoto() não entrega a foto
 *     quando o HFP está ativo — exatamente a combinação da StepWise
 *     (comando de voz via HFP, seguido de captura).
 *  2. A foto de capturePhoto() aparenta sair limitada a ~1080x1440 e
 *     ainda paga o tempo de transferência por Bluetooth; o stream a 7 fps
 *     já traz frames prontos, então dá pra escolher o melhor dos últimos
 *     ~1s em vez de esperar uma nova captura.
 *
 * `capturePhoto()` foi mantida apenas como plano B caso o stream não
 * esteja disponível por algum motivo.
 *
 * Regras de bateria (ver POLICIES no diagrama):
 *  - addCamera(...) em 720x1280.
 *  - fps do stream é configurável (STREAM_FPS_DEFAULT) — decidir com
 *    números reais medidos no evento (latência até o primeiro frame x
 *    consumo de bateria). Comentário da mentoria: se abrir/fechar a cada
 *    verificação custar vários segundos, considerar manter o stream a um
 *    fps mais baixo (ex.: 2 fps) durante toda a etapa.
 *  - camera.stop() imediatamente após a decisão do quality gate.
 *
 * TODO: substituir pelos tipos reais do SDK Meta DAT quando integrado
 * (com.meta.dat.*). Esta classe isola o restante do app da API do SDK.
 */
class MetaDatSessionManager(
    private val qualityGate: QualityGate = QualityGate()
) {

    /**
     * Abre (ou reaproveita, se já aberto) o stream de câmera, acumula os
     * frames da janela recente e retorna o melhor frame utilizável
     * segundo o QualityGate. Retorna null se nenhum frame do lote passar
     * no limiar mínimo (tratar como INCONCLUSIVE no motor procedural).
     *
     * IMPORTANTE: configurar/abrir o HFP *antes* de abrir o stream de
     * câmera — na ordem inversa o áudio pode falhar sem erro (feedback
     * da mentoria).
     */
    suspend fun captureBestFrameFromStream(windowMillis: Long = STREAM_WINDOW_MILLIS_DEFAULT): ByteArray? {
        // TODO: DeviceSession.addCamera(width = 720, height = 1280, fps = STREAM_FPS_DEFAULT)
        // TODO: stream.start() (idempotente se já estiver aberto)
        // TODO: acumular frames recebidos durante `windowMillis` num buffer curto
        // TODO: qualityGate.pickBest(bufferDeFrames)
        // TODO: medir e logar o tempo entre stream.start() e o primeiro frame
        //       (número que a mentoria pediu para decidir fps x latência).
        throw NotImplementedError("Integração com Meta DAT v0.9 (stream) pendente")
    }

    /**
     * Plano B: captura isolada via capturePhoto(). Evitar como caminho
     * principal enquanto o bug relatado (capturePhoto + HFP ativo) não
     * for confirmado/corrigido no Android.
     */
    suspend fun capturePhoto(): ByteArray {
        // TODO: camera.stop() imediato após a captura, se aberta só para isso.
        throw NotImplementedError("Integração com Meta DAT v0.9 (capturePhoto) pendente")
    }

    /** Encerra o stream de câmera. Chamar assim que o quality gate decidir. */
    fun stopStream() {
        // TODO: camera.stop()
    }

    companion object {
        const val STREAM_FPS_DEFAULT = 7
        const val STREAM_WINDOW_MILLIS_DEFAULT = 1_000L
    }
}

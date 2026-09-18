package com.stepwise.glasses

/**
 * Verifica se um frame é utilizável (não ocluído, não desfocado, com
 * evidência mínima) e, quando há mais de um frame candidato (stream),
 * escolhe o melhor entre eles — em vez de só aceitar/rejeitar um único
 * frame de capturePhoto().
 *
 * Motivo da mudança (feedback da mentoria, 15/09): há relato de que
 * capturePhoto() não entrega a foto quando o HFP está ativo — exatamente
 * a combinação da StepWise (comando de voz via HFP + captura). Como o
 * stream de câmera já fica aberto durante a janela de verificação, é
 * mais rápido e mais seguro escolher o melhor frame recente do stream do
 * que depender de uma chamada isolada de capturePhoto().
 *
 * Se nenhum frame do lote passar no limiar mínimo, o fluxo vai direto
 * para o motor procedural com o resultado INCONCLUSIVE (sem avançar
 * etapa, sem alterar nível de assistência).
 */
class QualityGate(
    private val minScoreThreshold: Float = MIN_SCORE_THRESHOLD_DEFAULT
) {

    /** Mantida para compatibilidade: uso simples com um único frame. */
    fun isUsable(frame: ByteArray): Boolean = score(frame) >= minScoreThreshold

    /**
     * Estima a qualidade de um frame (nitidez/exposição/oclusão) em uma
     * escala 0f..1f. TODO: heurística real (ex.: variância do Laplaciano
     * para nitidez, histograma para exposição, % de área com baixa
     * variação para oclusão) — hoje é um placeholder.
     */
    fun score(frame: ByteArray): Float {
        // TODO: implementar heurística real de qualidade de imagem.
        return if (frame.isNotEmpty()) 1f else 0f
    }

    /**
     * Escolhe o melhor frame entre os últimos capturados do stream
     * (ver MetaDatSessionManager.captureBestFrameFromStream). Retorna
     * null se nenhum frame atingir o limiar mínimo — nesse caso o
     * chamador deve tratar como INCONCLUSIVE.
     */
    fun pickBest(frames: List<ByteArray>): ByteArray? =
        frames
            .map { it to score(it) }
            .filter { (_, s) -> s >= minScoreThreshold }
            .maxByOrNull { (_, s) -> s }
            ?.first

    companion object {
        const val MIN_SCORE_THRESHOLD_DEFAULT = 0.5f
    }
}

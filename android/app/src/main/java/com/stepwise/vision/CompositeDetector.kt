package com.stepwise.vision

/**
 * Encadeia um detector principal (PLANO A — MediaPipe/LiteRT customizado)
 * com um detector determinístico de reserva (PLANO B — cor ou marcadores),
 * conforme recomendado pela mentoria: "o diferencial de vocês é o tutor
 * adaptativo; o detector pode ser a coisa mais simples que funcione."
 *
 * Estratégia: tenta o [primary]; se ele lançar exceção (modelo ainda não
 * treinado/carregado) ou devolver confiança abaixo de [minPrimaryConfidence]
 * para todas as detecções, usa o [fallback] no mesmo frame.
 *
 * Isso permite chegar ao evento com o pipeline inteiro funcionando mesmo
 * que o modelo customizado não esteja pronto/bom a tempo — sem trocar a
 * interface que o restante do app (ProceduralEngine, SessionController)
 * já usa.
 */
class CompositeDetector(
    private val primary: VisualDetector,
    private val fallback: VisualDetector,
    private val minPrimaryConfidence: Float = 0.5f
) : VisualDetector {

    override suspend fun detect(frame: ByteArray): DetectionResult {
        val primaryResult = try {
            primary.detect(frame)
        } catch (e: Exception) {
            null
        }

        val primaryIsGoodEnough = primaryResult != null &&
            primaryResult.detections.isNotEmpty() &&
            primaryResult.detections.all { it.confidence >= minPrimaryConfidence }

        return if (primaryIsGoodEnough) primaryResult!! else fallback.detect(frame)
    }
}

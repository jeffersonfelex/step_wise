package com.stepwise.glasses

/**
 * Verifica se o frame capturado é utilizável (não ocluído, não desfocado,
 * com evidência mínima) antes de enviá-lo ao detector visual.
 *
 * Se a imagem for inadequada, o fluxo vai direto para o motor procedural
 * com o resultado INCONCLUSIVE (sem avançar etapa, sem alterar nível).
 */
class QualityGate {

    fun isUsable(frame: ByteArray): Boolean {
        // TODO: heurísticas de nitidez/exposição/oclusão antes da inferência.
        return true
    }
}

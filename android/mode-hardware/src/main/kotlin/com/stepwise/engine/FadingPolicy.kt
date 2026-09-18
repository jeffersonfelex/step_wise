package com.stepwise.engine

/** Níveis de assistência, do mais detalhado (L4) ao mais sucinto (L1). */
enum class AssistanceLevel { L4, L3, L2, L1 }

/**
 * Política determinística de fading adaptativo.
 *
 * Regra do MVP: reduz um nível após N execuções consecutivas corretas, na
 * primeira tentativa e sem pedido de ajuda (HELP/REPEAT) na etapa.
 * Qualquer KNOWN_ERROR ou HELP zera o contador de acertos consecutivos.
 */
class FadingPolicy(private val consecutiveCorrectRequired: Int = 2) {

    fun nextLevel(current: AssistanceLevel, consecutiveCorrectStreak: Int): AssistanceLevel {
        if (consecutiveCorrectStreak < consecutiveCorrectRequired) return current
        return when (current) {
            AssistanceLevel.L4 -> AssistanceLevel.L3
            AssistanceLevel.L3 -> AssistanceLevel.L2
            AssistanceLevel.L2 -> AssistanceLevel.L1
            AssistanceLevel.L1 -> AssistanceLevel.L1
        }
    }
}

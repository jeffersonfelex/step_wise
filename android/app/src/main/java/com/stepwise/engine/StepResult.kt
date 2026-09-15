package com.stepwise.engine

/** Resultado da avaliação de uma etapa pelo motor procedural. */
enum class StepResult {
    CORRECT,
    KNOWN_ERROR,
    /** Evidência insuficiente: não avança etapa e não altera nível de assistência. */
    INCONCLUSIVE
}

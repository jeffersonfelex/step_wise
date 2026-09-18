package com.stepwise.engine

/** Resultado da avaliação de uma etapa pelo motor procedural. */
enum class StepResult {
    CORRECT,
    KNOWN_ERROR,
    /** Evidência insuficiente: não avança etapa e não altera nível de assistência. */
    INCONCLUSIVE
}

enum class DecisionReason {
    CORRECT_CLASS_CONFIDENT,
    KNOWN_ERROR_CONFIDENT,
    IMAGE_UNUSABLE,
    PROCEDURE_NOT_LOADED,
    COMPLETED,
    NO_DETECTIONS,
    LOW_CONFIDENCE,
    UNEXPECTED_CLASS
}

data class EngineDecision(
    val result: StepResult,
    val reason: DecisionReason,
    val message: String,
    val stepId: Int? = null,
    val stepName: String? = null,
    val observedClass: String? = null,
    val confidence: Float? = null
)

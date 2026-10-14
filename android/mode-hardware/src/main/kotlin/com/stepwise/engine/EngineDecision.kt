package com.stepwise.engine

/** Por que o motor chegou a um resultado; útil para feedback, logs e testes. */
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

/** Resultado da avaliação de uma etapa com a mensagem falada e a evidência usada. */
data class EngineDecision(
    val result: StepResult,
    val reason: DecisionReason,
    val message: String,
    val stepId: Int? = null,
    val stepName: String? = null,
    val observedClass: String? = null,
    val confidence: Float? = null
)

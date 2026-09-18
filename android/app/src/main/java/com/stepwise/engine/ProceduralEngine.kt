package com.stepwise.engine

import com.stepwise.procedure.Procedure
import com.stepwise.vision.DetectionResult

/**
 * Motor procedural (ENG no diagrama): estado + regras explícitas + fading.
 *
 * Entrada: detecções do VisualDetector + estado esperado da etapa (PROC).
 * Saída: StepResult (CORRECT / KNOWN_ERROR / INCONCLUSIVE), feedback para
 * TTS e atualização de estado persistente (Room).
 *
 * Não usa LLM no caminho crítico: toda decisão de avanço é determinística.
 */
class ProceduralEngine(
    private val fadingPolicy: FadingPolicy = FadingPolicy(),
    private val minimumConfidenceForCorrect: Float = 0.85f,
    private val minimumConfidenceForKnownError: Float = 0.85f
) {

    private var procedure: Procedure? = null
    private var currentStepIndex: Int = 0
    private var currentLevel: AssistanceLevel = AssistanceLevel.L4
    private var consecutiveCorrectStreak: Int = 0

    fun load(procedure: Procedure, resumeStepIndex: Int = 0, resumeLevel: AssistanceLevel = AssistanceLevel.L4) {
        this.procedure = procedure
        this.currentStepIndex = resumeStepIndex.coerceIn(0, procedure.steps.size)
        this.currentLevel = resumeLevel
        this.consecutiveCorrectStreak = 0
    }

    /** Confronta as detecções com as regras explícitas da etapa atual. */
    fun evaluate(detectionResult: DetectionResult, imageUsable: Boolean): StepResult =
        evaluateDecision(detectionResult, imageUsable).result

    fun evaluateDecision(detectionResult: DetectionResult, imageUsable: Boolean): EngineDecision {
        if (!imageUsable) {
            return inconclusive(DecisionReason.IMAGE_UNUSABLE, "Não consegui usar essa imagem. Vamos tentar verificar de novo.")
        }

        val loadedProcedure = procedure
            ?: return inconclusive(DecisionReason.PROCEDURE_NOT_LOADED, "Procedimento ainda não foi carregado.")

        val step = loadedProcedure.steps.getOrNull(currentStepIndex)
            ?: return EngineDecision(
                result = StepResult.INCONCLUSIVE,
                reason = DecisionReason.COMPLETED,
                message = "Procedimento já concluído.",
            )

        val best = detectionResult.detections.maxByOrNull { it.confidence }
            ?: return EngineDecision(
                result = StepResult.INCONCLUSIVE,
                reason = DecisionReason.NO_DETECTIONS,
                message = "Não identifiquei a montagem com confiança. Aproxime o conjunto e tente novamente.",
                stepId = step.stepId,
                stepName = step.name,
            )

        val knownError = step.knownErrors.firstOrNull { it.errorId == best.className }
        if (knownError != null && best.confidence >= minimumConfidenceForKnownError) {
            return EngineDecision(
                result = StepResult.KNOWN_ERROR,
                reason = DecisionReason.KNOWN_ERROR_CONFIDENT,
                message = knownError.message,
                stepId = step.stepId,
                stepName = step.name,
                observedClass = best.className,
                confidence = best.confidence,
            )
        }

        if (best.className in step.expectedClasses && best.confidence >= minimumConfidenceForCorrect) {
            return EngineDecision(
                result = StepResult.CORRECT,
                reason = DecisionReason.CORRECT_CLASS_CONFIDENT,
                message = "Etapa conferida. Pode continuar.",
                stepId = step.stepId,
                stepName = step.name,
                observedClass = best.className,
                confidence = best.confidence,
            )
        }

        val reason = if (best.confidence < minOf(minimumConfidenceForCorrect, minimumConfidenceForKnownError)) {
            DecisionReason.LOW_CONFIDENCE
        } else {
            DecisionReason.UNEXPECTED_CLASS
        }
        return EngineDecision(
            result = StepResult.INCONCLUSIVE,
            reason = reason,
            message = "Ainda não tenho evidência suficiente para avançar. Reposicione a montagem e verifique novamente.",
            stepId = step.stepId,
            stepName = step.name,
            observedClass = best.className,
            confidence = best.confidence,
        )
    }

    /** Aplica o resultado: avança etapa e/ou reduz nível conforme a política de fading. */
    fun applyResult(result: StepResult) {
        when (result) {
            StepResult.CORRECT -> {
                val loadedProcedure = procedure ?: return
                if (currentStepIndex >= loadedProcedure.steps.size) return
                consecutiveCorrectStreak += 1
                currentLevel = fadingPolicy.nextLevel(currentLevel, consecutiveCorrectStreak)
                if (consecutiveCorrectStreak >= 2) consecutiveCorrectStreak = 0
                currentStepIndex = (currentStepIndex + 1).coerceAtMost(loadedProcedure.steps.size)
                // TODO: persistir checkpoint em Room (step_id, nível, resultado).
            }
            StepResult.KNOWN_ERROR -> {
                consecutiveCorrectStreak = 0
                // TODO: persistir tentativa + mensagem de erro conhecido.
            }
            StepResult.INCONCLUSIVE -> {
                // Sem avanço, sem alterar nível, sem zerar streak.
            }
        }
    }

    fun currentInstruction(helpRequested: Boolean = false, repeat: Boolean = false): String {
        if (helpRequested) consecutiveCorrectStreak = 0
        val step = procedure?.steps?.getOrNull(currentStepIndex)
            ?: return "Procedimento concluído."
        val requestedLevel = if (helpRequested) AssistanceLevel.L4 else currentLevel
        return step.instructionsByLevel[requestedLevel.name]
            ?: step.instructionsByLevel[AssistanceLevel.L4.name]
            ?: "Siga para a próxima etapa."
    }

    fun currentStepNumber(): Int = currentStepIndex + 1

    fun isCompleted(): Boolean {
        val loadedProcedure = procedure ?: return false
        return currentStepIndex >= loadedProcedure.steps.size
    }

    private fun inconclusive(reason: DecisionReason, message: String): EngineDecision =
        EngineDecision(
            result = StepResult.INCONCLUSIVE,
            reason = reason,
            message = message,
        )
}

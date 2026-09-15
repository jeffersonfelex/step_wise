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
    private val fadingPolicy: FadingPolicy = FadingPolicy()
) {

    private var procedure: Procedure? = null
    private var currentStepIndex: Int = 0
    private var currentLevel: AssistanceLevel = AssistanceLevel.L4
    private var consecutiveCorrectStreak: Int = 0

    fun load(procedure: Procedure, resumeStepIndex: Int = 0, resumeLevel: AssistanceLevel = AssistanceLevel.L4) {
        this.procedure = procedure
        this.currentStepIndex = resumeStepIndex
        this.currentLevel = resumeLevel
        this.consecutiveCorrectStreak = 0
    }

    /** Confronta as detecções com as regras explícitas da etapa atual. */
    fun evaluate(detectionResult: DetectionResult, imageUsable: Boolean): StepResult {
        if (!imageUsable) return StepResult.INCONCLUSIVE

        // TODO: comparar detectionResult.detections com expected_classes/posição
        // relativa definidos em Procedure para a etapa atual.
        // TODO: checar known_errors da etapa antes de assumir CORRECT.

        return StepResult.INCONCLUSIVE
    }

    /** Aplica o resultado: avança etapa e/ou reduz nível conforme a política de fading. */
    fun applyResult(result: StepResult) {
        when (result) {
            StepResult.CORRECT -> {
                consecutiveCorrectStreak += 1
                currentLevel = fadingPolicy.nextLevel(currentLevel, consecutiveCorrectStreak)
                if (consecutiveCorrectStreak >= 2) consecutiveCorrectStreak = 0
                currentStepIndex += 1
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
        // TODO: buscar instructions[currentLevel] da etapa atual em Procedure.
        if (helpRequested) consecutiveCorrectStreak = 0
        return "TODO: instrução da etapa $currentStepIndex no nível $currentLevel"
    }
}

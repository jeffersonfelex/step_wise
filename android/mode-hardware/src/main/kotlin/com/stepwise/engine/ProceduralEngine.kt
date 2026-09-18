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

    /**
     * @param resumeStreak permite retomar já com N acertos consecutivos
     * registrados. Usado tanto na restauração real de checkpoint (Room)
     * quanto no modo demonstração (ver SessionController.enableDemoMode),
     * para mostrar a transição de fading (ex.: L3 -> L2) em poucos
     * minutos de pitch em vez de exigir duas execuções corretas do zero.
     */
    fun load(
        procedure: Procedure,
        resumeStepIndex: Int = 0,
        resumeLevel: AssistanceLevel = AssistanceLevel.L4,
        resumeStreak: Int = 0
    ) {
        this.procedure = procedure
        this.currentStepIndex = resumeStepIndex
        this.currentLevel = resumeLevel
        this.consecutiveCorrectStreak = resumeStreak
    }

    /** Confronta as detecções com as regras explícitas da etapa atual. */
    fun evaluate(detectionResult: DetectionResult, imageUsable: Boolean): StepResult {
        if (!imageUsable) return StepResult.INCONCLUSIVE

        // TODO: comparar detectionResult.detections com expected_classes/posição
        // relativa definidos em Procedure para a etapa atual.
        // TODO: checar known_errors da etapa antes de assumir CORRECT.

        return StepResult.INCONCLUSIVE
    }

    /**
     * Aplica o resultado: avança etapa e/ou reduz nível conforme a
     * política de fading. Também usado diretamente pelo modo demonstração
     * para forçar um KNOWN_ERROR ou INCONCLUSIVE roteirizado no pitch,
     * sem depender da detecção visual real.
     */
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

    fun currentLevel(): AssistanceLevel = currentLevel

    fun currentStepIndex(): Int = currentStepIndex

    /** Etapa atual do procedimento carregado, ou null se já concluiu ou nada foi carregado. */
    fun currentStep() = procedure?.steps?.getOrNull(currentStepIndex)

    /**
     * Instrução falada para a etapa/nível atuais (lida do Procedure
     * carregado via ProcedureLoader). Pedir ajuda (HELP) zera o streak de
     * acertos consecutivos, como qualquer pedido de ajuda faz na política
     * de fading — repetir (REPEAT) não zera, é só reler a última instrução.
     */
    fun currentInstruction(helpRequested: Boolean = false, repeat: Boolean = false): String {
        if (helpRequested) consecutiveCorrectStreak = 0

        val proc = procedure ?: return "Nenhum procedimento carregado."
        val step = proc.steps.getOrNull(currentStepIndex)
            ?: return "Procedimento concluído. Parabéns!"

        return step.instructionsByLevel[currentLevel.name]
            ?: step.instructionsByLevel.values.firstOrNull()
            ?: "Instrução indisponível para a etapa ${step.stepId}."
    }
}

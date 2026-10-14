package com.stepwise.engine

import com.stepwise.procedure.Procedure
import com.stepwise.procedure.ProcedureStep
import com.stepwise.vision.DetectionResult

/**
 * Motor procedural (ENG no diagrama): estado + regras explícitas + fading.
 *
 * Entrada: detecções do VisualDetector + estado esperado da etapa (PROC).
 * Saída: EngineDecision (CORRECT / KNOWN_ERROR / INCONCLUSIVE com motivo e
 * mensagem), feedback para TTS e atualização de estado.
 *
 * A decisão é determinística e só aceita CORRECT ou KNOWN_ERROR acima do
 * limiar de confiança; abaixo dele, abstém-se (INCONCLUSIVE). Avaliadores
 * externos (ex.: LlamaStepEvaluator) só entram quando o motor se abstém —
 * ver SessionController.
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
        this.currentStepIndex = resumeStepIndex.coerceIn(0, procedure.steps.size)
        this.currentLevel = resumeLevel
        this.consecutiveCorrectStreak = resumeStreak.coerceAtLeast(0)
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
            ?: return inconclusive(DecisionReason.COMPLETED, "Procedimento já concluído.")

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

    /**
     * Aplica o resultado: avança etapa e/ou reduz nível conforme a
     * política de fading. Também usado diretamente pelo modo demonstração
     * para forçar um KNOWN_ERROR ou INCONCLUSIVE roteirizado no pitch,
     * sem depender da detecção visual real.
     */
    fun applyResult(result: StepResult) {
        when (result) {
            StepResult.CORRECT -> {
                val loadedProcedure = procedure ?: return
                if (currentStepIndex >= loadedProcedure.steps.size) return
                consecutiveCorrectStreak += 1
                val nextLevel = fadingPolicy.nextLevel(currentLevel, consecutiveCorrectStreak)
                // Zera ao mudar de nível: a próxima redução exige uma nova sequência
                // completa, qualquer que seja o tamanho configurado na FadingPolicy.
                if (nextLevel != currentLevel) consecutiveCorrectStreak = 0
                currentLevel = nextLevel
                currentStepIndex += 1
            }
            StepResult.KNOWN_ERROR -> {
                consecutiveCorrectStreak = 0
            }
            StepResult.INCONCLUSIVE -> {
                // Sem avanço, sem alterar nível, sem zerar streak.
            }
        }
    }

    fun procedureId(): String? = procedure?.procedureId

    fun currentLevel(): AssistanceLevel = currentLevel

    fun currentStepIndex(): Int = currentStepIndex

    fun currentStepNumber(): Int = currentStepIndex + 1

    /** Etapa atual do procedimento carregado, ou null se já concluiu ou nada foi carregado. */
    fun currentStep(): ProcedureStep? = procedure?.steps?.getOrNull(currentStepIndex)

    fun isCompleted(): Boolean {
        val loadedProcedure = procedure ?: return false
        return currentStepIndex >= loadedProcedure.steps.size
    }

    /**
     * Instrução falada para a etapa atual. Pedir ajuda (HELP) devolve a versão
     * mais detalhada (L4) e zera o streak de acertos, como na política de
     * fading; repetir (REPEAT) só relê a instrução do nível atual.
     */
    fun currentInstruction(helpRequested: Boolean = false, repeat: Boolean = false): String {
        if (helpRequested) consecutiveCorrectStreak = 0

        val proc = procedure ?: return "Nenhum procedimento carregado."
        val step = proc.steps.getOrNull(currentStepIndex)
            ?: return "Procedimento concluído. Parabéns!"

        val requestedLevel = if (helpRequested) AssistanceLevel.L4 else currentLevel
        return step.instructionsByLevel[requestedLevel.name]
            ?: step.instructionsByLevel[AssistanceLevel.L4.name]
            ?: step.instructionsByLevel.values.firstOrNull()
            ?: "Instrução indisponível para a etapa ${step.stepId}."
    }

    private fun inconclusive(reason: DecisionReason, message: String): EngineDecision =
        EngineDecision(
            result = StepResult.INCONCLUSIVE,
            reason = reason,
            message = message,
        )
}

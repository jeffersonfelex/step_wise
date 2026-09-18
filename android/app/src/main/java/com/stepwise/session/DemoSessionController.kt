package com.stepwise.session

import com.stepwise.engine.EngineDecision
import com.stepwise.engine.ProceduralEngine
import com.stepwise.engine.StepResult
import com.stepwise.procedure.KnownError
import com.stepwise.procedure.Procedure
import com.stepwise.procedure.ProcedureStep
import com.stepwise.vision.BoundingBox
import com.stepwise.vision.Detection
import com.stepwise.vision.DetectionResult
import com.stepwise.vision.StepwiseImageClassifier

data class DemoSessionState(
    val title: String,
    val stepText: String,
    val instruction: String,
    val lastDecision: String,
    val completed: Boolean,
)

class DemoSessionController(
    private val engine: ProceduralEngine = ProceduralEngine()
) {
    private val procedure = torneiraProcedure()
    private var lastDecision: EngineDecision? = null

    init {
        engine.load(procedure)
    }

    fun state(): DemoSessionState {
        val completed = engine.isCompleted()
        val stepText = if (completed) {
            "Procedimento concluído"
        } else {
            "Etapa ${engine.currentStepNumber()} de ${procedure.steps.size}"
        }
        return DemoSessionState(
            title = procedure.title,
            stepText = stepText,
            instruction = engine.currentInstruction(),
            lastDecision = lastDecision?.let(::formatDecision)
                ?: "Nenhuma verificação ainda. Use os botões para simular uma evidência controlada.",
            completed = completed,
        )
    }

    fun verifyCorrect(): DemoSessionState {
        val step = currentStepOrNull()
        val className = step?.expectedClasses?.firstOrNull() ?: "S6_final"
        return evaluateAndApply(className, confidence = 0.95f)
    }

    fun verifyKnownError(): DemoSessionState {
        val step = currentStepOrNull()
        val className = step?.knownErrors?.firstOrNull()?.errorId ?: "E1_ordem_errada"
        return evaluateAndApply(className, confidence = 0.95f)
    }

    fun verifyInconclusive(): DemoSessionState {
        return evaluateAndApply("classe_desconhecida", confidence = 0.20f)
    }

    fun expectedVisualSampleForCurrentStep(): StepwiseImageClassifier.AssetSample? {
        val expectedClass = currentStepOrNull()?.expectedClasses?.firstOrNull() ?: return null
        return StepwiseImageClassifier.sampleForExpectedLabel(expectedClass)
    }

    fun verifyVisualClassification(topClass: String, confidence: Float): DemoSessionState {
        return evaluateAndApply(topClass, confidence)
    }

    fun help(): DemoSessionState {
        lastDecision = null
        return state().copy(instruction = engine.currentInstruction(helpRequested = true))
    }

    fun repeat(): DemoSessionState {
        lastDecision = null
        return state().copy(instruction = engine.currentInstruction(repeat = true))
    }

    fun reset(): DemoSessionState {
        engine.load(procedure)
        lastDecision = null
        return state()
    }

    private fun evaluateAndApply(className: String, confidence: Float): DemoSessionState {
        if (engine.isCompleted()) return state()
        val decision = engine.evaluateDecision(
            DetectionResult(
                detections = listOf(
                    Detection(
                        className = className,
                        confidence = confidence,
                        boundingBox = BoundingBox(0.1f, 0.1f, 0.9f, 0.9f),
                    )
                )
            ),
            imageUsable = true,
        )
        lastDecision = decision
        engine.applyResult(decision.result)
        return state()
    }

    private fun currentStepOrNull(): ProcedureStep? =
        procedure.steps.getOrNull(engine.currentStepNumber() - 1)

    private fun formatDecision(decision: EngineDecision): String {
        val status = when (decision.result) {
            StepResult.CORRECT -> "CORRECT"
            StepResult.KNOWN_ERROR -> "KNOWN_ERROR"
            StepResult.INCONCLUSIVE -> "INCONCLUSIVE"
        }
        val observed = decision.observedClass?.let {
            " · classe=$it · confiança=${"%.2f".format(decision.confidence ?: 0f)}"
        }.orEmpty()
        return "$status · ${decision.reason}$observed\n${decision.message}"
    }

    private companion object {
        fun torneiraProcedure(): Procedure = Procedure(
            procedureId = "montagem_torneira_stepwise_v1",
            title = "Montagem de Torneira com Conexões",
            steps = listOf(
                step(
                    id = 1,
                    name = "Separar e orientar a torneira",
                    expected = "S1_torneira",
                    l4 = "Pegue a torneira principal. Deixe a rosca lateral voltada para a sequência de montagem.",
                    l3 = "Prepare a torneira com a rosca lateral livre.",
                    l2 = "Prepare a torneira.",
                    l1 = "Torneira.",
                    errors = listOf("E2_peca_invertida", "E4_peca_solta"),
                ),
                step(
                    id = 2,
                    name = "Enroscar P2 na torneira",
                    expected = "S2_torneira_P2",
                    l4 = "Pegue a P2 e enrosque na saída lateral da torneira até ficar alinhada.",
                    l3 = "Enrosque a P2 na saída lateral.",
                    l2 = "Coloque a P2.",
                    l1 = "P2.",
                    errors = listOf("E1_ordem_errada", "E2_peca_invertida", "E4_peca_solta"),
                ),
                step(
                    id = 3,
                    name = "Enroscar P3 no conjunto",
                    expected = "S3_torneira_P2_P3",
                    l4 = "Conecte a P3 depois da P2, mantendo o conjunto reto.",
                    l3 = "Conecte a P3 depois da P2.",
                    l2 = "Coloque a P3.",
                    l1 = "P3.",
                    errors = listOf("E1_ordem_errada", "E3_peca_ausente", "E4_peca_solta"),
                ),
                step(
                    id = 4,
                    name = "Enroscar P4 no conjunto",
                    expected = "S4_torneira_P2_P3_P4",
                    l4 = "Enrosque a P4 depois da P3. Confira se ela entrou reta.",
                    l3 = "Enrosque a P4 depois da P3.",
                    l2 = "Coloque a P4.",
                    l1 = "P4.",
                    errors = listOf("E1_ordem_errada", "E2_peca_invertida", "E3_peca_ausente"),
                ),
                step(
                    id = 5,
                    name = "Enroscar P5 no conjunto",
                    expected = "S5_torneira_P2_P3_P4_P5",
                    l4 = "Conecte a P5 depois da P4. Pare quando estiver firme.",
                    l3 = "Conecte a P5 depois da P4.",
                    l2 = "Coloque a P5.",
                    l1 = "P5.",
                    errors = listOf("E1_ordem_errada", "E3_peca_ausente", "E4_peca_solta"),
                ),
                step(
                    id = 6,
                    name = "Finalizar com P6",
                    expected = "S6_final",
                    l4 = "Faça a conexão final com a P6 e confira se todas as peças estão na sequência correta.",
                    l3 = "Conecte a P6 e confira o conjunto final.",
                    l2 = "Finalize com a P6.",
                    l1 = "P6.",
                    errors = listOf("E1_ordem_errada", "E3_peca_ausente", "E4_peca_solta"),
                ),
            ),
        )

        fun step(
            id: Int,
            name: String,
            expected: String,
            l4: String,
            l3: String,
            l2: String,
            l1: String,
            errors: List<String>,
        ) = ProcedureStep(
            stepId = id,
            name = name,
            expectedClasses = listOf(expected),
            instructionsByLevel = mapOf("L4" to l4, "L3" to l3, "L2" to l2, "L1" to l1),
            knownErrors = errors.map { KnownError(it, errorMessage(it)) },
        )

        fun errorMessage(errorId: String): String = when (errorId) {
            "E1_ordem_errada" -> "A ordem parece incorreta. Volte para a peça esperada nesta etapa."
            "E2_peca_invertida" -> "A peça parece invertida. Vire a orientação antes de continuar."
            "E3_peca_ausente" -> "Parece faltar uma peça no conjunto. Confira a etapa atual."
            "E4_peca_solta" -> "A peça parece solta. Enrosque até ficar firme antes de verificar."
            else -> "Erro conhecido detectado. Corrija a montagem antes de avançar."
        }
    }
}

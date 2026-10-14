package com.stepwise.engine

import com.stepwise.procedure.KnownError
import com.stepwise.procedure.Procedure
import com.stepwise.procedure.ProcedureStep
import com.stepwise.vision.BoundingBox
import com.stepwise.vision.Detection
import com.stepwise.vision.DetectionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProceduralEngineTest {

    @Test
    fun `imagem inutilizavel retorna INCONCLUSIVE`() {
        val engine = ProceduralEngine()
        val result = engine.evaluate(DetectionResult(detections = emptyList()), imageUsable = false)
        assertEquals(StepResult.INCONCLUSIVE, result)
    }

    @Test
    fun `sem procedimento carregado retorna INCONCLUSIVE`() {
        val decision = ProceduralEngine().evaluateDecision(result("S1_torneira", 0.99f), imageUsable = true)
        assertEquals(DecisionReason.PROCEDURE_NOT_LOADED, decision.reason)
    }

    @Test
    fun `classe esperada com confianca alta retorna CORRECT`() {
        val engine = ProceduralEngine()
        engine.load(procedure())

        val decision = engine.evaluateDecision(result("S1_torneira", 0.93f), imageUsable = true)

        assertEquals(StepResult.CORRECT, decision.result)
        assertEquals(DecisionReason.CORRECT_CLASS_CONFIDENT, decision.reason)
        assertEquals(1, decision.stepId)
    }

    @Test
    fun `erro conhecido com confianca alta retorna KNOWN_ERROR sem avancar`() {
        val engine = ProceduralEngine()
        engine.load(procedure())

        val decision = engine.evaluateDecision(result("E2_peca_invertida", 0.91f), imageUsable = true)
        engine.applyResult(decision.result)

        assertEquals(StepResult.KNOWN_ERROR, decision.result)
        assertEquals(DecisionReason.KNOWN_ERROR_CONFIDENT, decision.reason)
        assertEquals("A peça parece invertida.", decision.message)
        assertEquals(1, engine.currentStepNumber())
    }

    @Test
    fun `classe esperada com baixa confianca retorna INCONCLUSIVE`() {
        val engine = ProceduralEngine()
        engine.load(procedure())

        val decision = engine.evaluateDecision(result("S1_torneira", 0.40f), imageUsable = true)

        assertEquals(StepResult.INCONCLUSIVE, decision.result)
        assertEquals(DecisionReason.LOW_CONFIDENCE, decision.reason)
    }

    @Test
    fun `classe de outra etapa com confianca alta e INCONCLUSIVE`() {
        val engine = ProceduralEngine()
        engine.load(procedure())

        val decision = engine.evaluateDecision(result("S2_torneira_P2", 0.95f), imageUsable = true)

        assertEquals(StepResult.INCONCLUSIVE, decision.result)
        assertEquals(DecisionReason.UNEXPECTED_CLASS, decision.reason)
    }

    @Test
    fun `applyResult avanca ate concluir sem criar etapa extra`() {
        val engine = ProceduralEngine()
        engine.load(procedure())

        assertFalse(engine.isCompleted())
        engine.applyResult(StepResult.CORRECT)
        assertEquals(2, engine.currentStepNumber())
        engine.applyResult(StepResult.CORRECT)
        assertTrue(engine.isCompleted())
        assertNull(engine.currentStep())
        engine.applyResult(StepResult.CORRECT)
        assertTrue(engine.isCompleted())
        assertEquals(DecisionReason.COMPLETED, engine.evaluateDecision(result("S6_final", 0.99f), true).reason)
    }

    @Test
    fun `currentInstruction usa o nivel atual e HELP volta para L4`() {
        val engine = ProceduralEngine()
        engine.load(procedure(), resumeLevel = AssistanceLevel.L3)

        assertEquals("Prepare a torneira.", engine.currentInstruction())
        assertEquals("Pegue a torneira principal e deixe a rosca lateral pronta.", engine.currentInstruction(helpRequested = true))
    }

    @Test
    fun `duas etapas corretas seguidas reduzem um nivel`() {
        val engine = ProceduralEngine()
        engine.load(longProcedure())

        engine.applyResult(StepResult.CORRECT)
        assertEquals(AssistanceLevel.L4, engine.currentLevel())
        engine.applyResult(StepResult.CORRECT)
        assertEquals(AssistanceLevel.L3, engine.currentLevel())
        engine.applyResult(StepResult.CORRECT)
        assertEquals(AssistanceLevel.L3, engine.currentLevel())
        engine.applyResult(StepResult.CORRECT)
        assertEquals(AssistanceLevel.L2, engine.currentLevel())
    }

    @Test
    fun `fading respeita a sequencia configurada na politica`() {
        val engine = ProceduralEngine(fadingPolicy = FadingPolicy(consecutiveCorrectRequired = 3))
        engine.load(longProcedure())

        repeat(2) { engine.applyResult(StepResult.CORRECT) }
        assertEquals(AssistanceLevel.L4, engine.currentLevel())
        engine.applyResult(StepResult.CORRECT)
        assertEquals(AssistanceLevel.L3, engine.currentLevel())
    }

    @Test
    fun `erro conhecido e pedido de ajuda zeram a sequencia de acertos`() {
        val engine = ProceduralEngine()
        engine.load(longProcedure())

        engine.applyResult(StepResult.CORRECT)
        engine.applyResult(StepResult.KNOWN_ERROR)
        engine.applyResult(StepResult.CORRECT)
        assertEquals(AssistanceLevel.L4, engine.currentLevel())

        engine.currentInstruction(helpRequested = true)
        engine.applyResult(StepResult.CORRECT)
        assertEquals(AssistanceLevel.L4, engine.currentLevel())
    }

    @Test
    fun `modo demonstracao retoma a um acerto da troca de nivel`() {
        val engine = ProceduralEngine()
        engine.load(longProcedure(), resumeLevel = AssistanceLevel.L3, resumeStreak = 1)

        engine.applyResult(StepResult.CORRECT)

        assertEquals(AssistanceLevel.L2, engine.currentLevel())
    }

    @Test
    fun `INCONCLUSIVE nao avanca nem altera nivel`() {
        val engine = ProceduralEngine()
        engine.load(procedure(), resumeLevel = AssistanceLevel.L2)

        engine.applyResult(StepResult.INCONCLUSIVE)

        assertEquals(1, engine.currentStepNumber())
        assertEquals(AssistanceLevel.L2, engine.currentLevel())
    }

    private fun procedure(): Procedure = Procedure(
        procedureId = "montagem_torneira_stepwise_v1",
        title = "Montagem de Torneira",
        steps = listOf(
            ProcedureStep(
                stepId = 1,
                name = "Separar e orientar a torneira",
                expectedClasses = listOf("S1_torneira"),
                instructionsByLevel = mapOf(
                    "L4" to "Pegue a torneira principal e deixe a rosca lateral pronta.",
                    "L3" to "Prepare a torneira.",
                    "L2" to "Torneira.",
                    "L1" to "Torneira.",
                ),
                knownErrors = listOf(
                    KnownError("E2_peca_invertida", "A peça parece invertida.")
                ),
            ),
            ProcedureStep(
                stepId = 2,
                name = "Enroscar P2",
                expectedClasses = listOf("S2_torneira_P2"),
                instructionsByLevel = mapOf(
                    "L4" to "Enrosque a P2 na torneira.",
                    "L3" to "Coloque a P2.",
                ),
                knownErrors = emptyList(),
            ),
        ),
    )

    private fun longProcedure(): Procedure = Procedure(
        procedureId = "longo",
        title = "Procedimento longo",
        steps = (1..8).map { id ->
            ProcedureStep(id, "Etapa $id", listOf("S$id"), mapOf("L4" to "Faça a etapa $id."), emptyList())
        },
    )

    private fun result(className: String, confidence: Float): DetectionResult =
        DetectionResult(
            detections = listOf(
                Detection(
                    className = className,
                    confidence = confidence,
                    boundingBox = BoundingBox(0.1f, 0.1f, 0.9f, 0.9f),
                )
            )
        )
}

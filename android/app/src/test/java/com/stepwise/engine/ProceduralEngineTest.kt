package com.stepwise.engine

import com.stepwise.procedure.KnownError
import com.stepwise.procedure.Procedure
import com.stepwise.procedure.ProcedureStep
import com.stepwise.vision.BoundingBox
import com.stepwise.vision.Detection
import com.stepwise.vision.DetectionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `applyResult avanca ate concluir sem criar setima etapa`() {
        val engine = ProceduralEngine()
        engine.load(procedure())

        assertFalse(engine.isCompleted())
        engine.applyResult(StepResult.CORRECT)
        assertEquals(2, engine.currentStepNumber())
        engine.applyResult(StepResult.CORRECT)
        assertTrue(engine.isCompleted())
        engine.applyResult(StepResult.CORRECT)
        assertTrue(engine.isCompleted())
    }

    @Test
    fun `currentInstruction usa mensagens do procedimento e HELP volta para L4`() {
        val engine = ProceduralEngine()
        engine.load(procedure())

        assertEquals("Pegue a torneira principal e deixe a rosca lateral pronta.", engine.currentInstruction())
        assertEquals("Pegue a torneira principal e deixe a rosca lateral pronta.", engine.currentInstruction(helpRequested = true))
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

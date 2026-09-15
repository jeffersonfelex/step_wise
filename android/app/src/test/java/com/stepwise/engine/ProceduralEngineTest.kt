package com.stepwise.engine

import com.stepwise.vision.DetectionResult
import org.junit.Assert.assertEquals
import org.junit.Test

class ProceduralEngineTest {

    @Test
    fun `imagem inutilizavel retorna INCONCLUSIVE`() {
        val engine = ProceduralEngine()
        val result = engine.evaluate(DetectionResult(detections = emptyList()), imageUsable = false)
        assertEquals(StepResult.INCONCLUSIVE, result)
    }
}

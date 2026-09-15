package com.stepwise.vision

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeDetector(
    private val result: DetectionResult? = null,
    private val shouldThrow: Boolean = false
) : VisualDetector {
    override suspend fun detect(frame: ByteArray): DetectionResult {
        if (shouldThrow) throw IllegalStateException("modelo indisponível")
        return result ?: DetectionResult(emptyList())
    }
}

class CompositeDetectorTest {

    private val highConfidenceDetection = DetectionResult(
        detections = listOf(
            Detection(
                className = "engrenagem_azul",
                confidence = 0.9f,
                boundingBox = BoundingBox(0f, 0f, 0.5f, 0.5f)
            )
        )
    )

    private val lowConfidenceDetection = DetectionResult(
        detections = listOf(
            Detection(
                className = "engrenagem_azul",
                confidence = 0.1f,
                boundingBox = BoundingBox(0f, 0f, 0.5f, 0.5f)
            )
        )
    )

    @Test
    fun `usa o detector primario quando a confianca e suficiente`() = runTest {
        val composite = CompositeDetector(
            primary = FakeDetector(result = highConfidenceDetection),
            fallback = FakeDetector(result = DetectionResult(emptyList()))
        )

        val result = composite.detect(ByteArray(0))

        assertEquals(highConfidenceDetection, result)
    }

    @Test
    fun `cai para o fallback quando o primario lanca exceção`() = runTest {
        val fallbackResult = DetectionResult(
            detections = listOf(
                Detection("engrenagem_azul", 1f, BoundingBox(0f, 0f, 1f, 1f))
            )
        )
        val composite = CompositeDetector(
            primary = FakeDetector(shouldThrow = true),
            fallback = FakeDetector(result = fallbackResult)
        )

        val result = composite.detect(ByteArray(0))

        assertEquals(fallbackResult, result)
    }

    @Test
    fun `cai para o fallback quando a confianca do primario e baixa`() = runTest {
        val fallbackResult = DetectionResult(
            detections = listOf(
                Detection("engrenagem_azul", 1f, BoundingBox(0f, 0f, 1f, 1f))
            )
        )
        val composite = CompositeDetector(
            primary = FakeDetector(result = lowConfidenceDetection),
            fallback = FakeDetector(result = fallbackResult),
            minPrimaryConfidence = 0.5f
        )

        val result = composite.detect(ByteArray(0))

        assertEquals(fallbackResult, result)
    }
}

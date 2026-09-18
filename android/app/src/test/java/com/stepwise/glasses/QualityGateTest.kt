package com.stepwise.glasses

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityGateTest {

    @Test
    fun `pickBest retorna null quando nenhum frame passa no limiar`() {
        val gate = QualityGate(minScoreThreshold = 2f) // limiar inatingível de propósito
        val result = gate.pickBest(listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6)))
        assertNull(result)
    }

    @Test
    fun `pickBest retorna um frame quando o limiar e atingivel`() {
        val gate = QualityGate(minScoreThreshold = 0.5f)
        val chosen = gate.pickBest(listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6)))
        assertTrue(chosen != null)
    }

    @Test
    fun `isUsable segue o mesmo limiar de score`() {
        val gate = QualityGate(minScoreThreshold = 0.5f)
        assertTrue(gate.isUsable(byteArrayOf(1)))
    }
}

package com.stepwise.software

import com.stepwise.core.FrameSource
import com.stepwise.core.GoalAssessment
import com.stepwise.core.ImageFrame
import com.stepwise.core.ImageSource
import com.stepwise.core.PcmAudioChunk
import com.stepwise.engine.StepResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SoftwareTutorSessionTest {
    private val drawing = frame(ImageSource.IMPORTED_DRAWING)
    private val task = CadTask(drawing, "Modelar suporte com dois furos")

    @Test
    fun `opens provider with drawing and verifies current goal using glasses`() = runTest {
        val connection = RecordingConnection()
        val captured = frame(ImageSource.GLASSES)
        val session = SoftwareTutorSession.open(
            provider = { received ->
                assertSame(task, received)
                connection
            },
            task = task,
            frames = { captured },
        )

        assertTrue(session.verify("sketch"))
        val verification = connection.inputs.single() as SoftwareTutorInput.Verify
        assertSame(captured, verification.frame)
        assertEquals("sketch", verification.goalId)
    }

    @Test
    fun `missing frame never sends verification to model`() = runTest {
        val connection = RecordingConnection()
        val session = open(connection, FrameSource { null })
        assertFalse(session.verify("sketch"))
        assertTrue(connection.inputs.isEmpty())
    }

    @Test
    fun `streams audio and assessment without depending on Android or physical detectors`() = runTest {
        val audio = PcmAudioChunk(byteArrayOf(0, 0), 24_000)
        val assessment = GoalAssessment("sketch", StepResult.INCONCLUSIVE, "Aproxime a cota")
        val expected = listOf(
            SoftwareTutorEvent.Audio(audio),
            SoftwareTutorEvent.Assessment(assessment),
            SoftwareTutorEvent.TurnCompleted,
        )
        val connection = RecordingConnection(flowOf(*expected.toTypedArray()))
        val session = open(connection)
        val input = SoftwareTutorInput.Audio(PcmAudioChunk(byteArrayOf(0, 0), 16_000))
        session.send(input)
        session.send(SoftwareTutorInput.Interrupt)
        assertEquals(listOf(input, SoftwareTutorInput.Interrupt), connection.inputs)
        assertEquals(expected, session.events.toList())
    }

    @Test
    fun `closing releases provider once and blocks future capture`() = runTest {
        val connection = RecordingConnection()
        var captures = 0
        val session = open(connection, FrameSource { captures++; frame() })
        session.close()
        session.close()
        assertEquals(1, connection.closes)
        try {
            session.verify("sketch")
            fail("Closed session must reject verification")
        } catch (_: IllegalStateException) {
            assertEquals(0, captures)
        }
        assertTrue(connection.inputs.isEmpty())
    }

    private suspend fun open(
        connection: RecordingConnection,
        frames: FrameSource = FrameSource { frame() },
    ) = SoftwareTutorSession.open({ connection }, task, frames)

    private fun frame(source: ImageSource = ImageSource.TEST_FIXTURE) =
        ImageFrame(byteArrayOf(1), "image/jpeg", source, 0)

    private class RecordingConnection(
        override val events: Flow<SoftwareTutorEvent> = flowOf(),
    ) : SoftwareTutorConnection {
        val inputs = mutableListOf<SoftwareTutorInput>()
        var closes = 0
        override suspend fun send(input: SoftwareTutorInput) { inputs += input }
        override suspend fun close() { closes++ }
    }
}

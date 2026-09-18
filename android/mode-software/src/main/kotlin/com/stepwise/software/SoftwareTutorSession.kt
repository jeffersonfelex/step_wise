package com.stepwise.software

import com.stepwise.core.FrameSource
import kotlinx.coroutines.flow.Flow

/**
 * Provider-independent session. Call lifecycle/input methods serially from the owner
 * coroutine; collect events concurrently. This class neither plays audio nor advances
 * goals: the app owns those decisions. No real provider is configured by default.
 */
class SoftwareTutorSession private constructor(
    private val connection: SoftwareTutorConnection,
    private val frames: FrameSource,
) {
    private var closed = false
    val events: Flow<SoftwareTutorEvent> get() = connection.events

    suspend fun send(input: SoftwareTutorInput) {
        check(!closed) { "Sessão encerrada" }
        connection.send(input)
    }

    /** False means no usable capture: do not ask the model to verify an unseen state. */
    suspend fun verify(goalId: String): Boolean {
        check(!closed) { "Sessão encerrada" }
        require(goalId.isNotBlank())
        val frame = frames.capture() ?: return false
        send(SoftwareTutorInput.Verify(frame, goalId))
        return true
    }

    suspend fun close() {
        if (closed) return
        closed = true
        connection.close()
    }

    companion object {
        suspend fun open(
            provider: SoftwareTutorProvider,
            task: CadTask,
            frames: FrameSource,
        ): SoftwareTutorSession = SoftwareTutorSession(provider.connect(task), frames)
    }
}

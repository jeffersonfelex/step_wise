package com.stepwise.software

import com.stepwise.core.GoalAssessment
import com.stepwise.core.ImageFrame
import com.stepwise.core.PcmAudioChunk
import com.stepwise.core.TutoringPlan
import kotlinx.coroutines.flow.Flow

data class CadTask(
    val drawing: ImageFrame,
    val objective: String,
    val applicationName: String = "Tinkercad",
) {
    init {
        require(objective.isNotBlank())
        require(applicationName.isNotBlank())
    }
}

/** Implement this in a provider adapter; no cloud SDK or credentials in the domain. */
fun interface SoftwareTutorProvider {
    suspend fun connect(task: CadTask): SoftwareTutorConnection
}

interface SoftwareTutorConnection {
    /** Collect before sending inputs. Implementations must release resources on close. */
    val events: Flow<SoftwareTutorEvent>
    suspend fun send(input: SoftwareTutorInput)
    suspend fun close()
}

sealed interface SoftwareTutorInput {
    data class Text(val text: String) : SoftwareTutorInput
    data class Audio(val chunk: PcmAudioChunk) : SoftwareTutorInput
    /** Atomic verification input prevents pairing a request with another frame. */
    data class Verify(val frame: ImageFrame, val goalId: String) : SoftwareTutorInput
    data object Interrupt : SoftwareTutorInput
}

sealed interface SoftwareTutorEvent {
    data class TextDelta(val text: String) : SoftwareTutorEvent
    data class Audio(val chunk: PcmAudioChunk) : SoftwareTutorEvent
    /** The application reviews/accepts the plan and owns progress, not the provider. */
    data class PlanProposed(val plan: TutoringPlan) : SoftwareTutorEvent
    data class Assessment(val assessment: GoalAssessment) : SoftwareTutorEvent
    data class Failure(val message: String) : SoftwareTutorEvent
    data object TurnCompleted : SoftwareTutorEvent
}

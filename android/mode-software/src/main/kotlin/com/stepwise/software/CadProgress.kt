package com.stepwise.software

import com.stepwise.core.GoalAssessment
import com.stepwise.core.TutoringPlan
import com.stepwise.engine.StepResult
import java.util.UUID

/** Local source of truth. Model output can propose a plan or assess evidence, never advance. */
class CadProgress {
    var proposedPlan: TutoringPlan? = null
        private set
    var plan: TutoringPlan? = null
        private set
    var index: Int = 0
        private set
    var assessment: GoalAssessment? = null
        private set
    var pendingRequestId: String? = null
        private set
    val currentGoal get() = plan?.goals?.getOrNull(index)
    val completed get() = plan != null && currentGoal == null
    val canAdvance get() = currentGoal != null && pendingRequestId == null &&
        assessment?.result == StepResult.CORRECT

    fun propose(candidate: TutoringPlan): Boolean {
        if (plan != null) return false // A later model proposal must not replace accepted progress.
        proposedPlan = candidate
        return true
    }

    fun acceptPlan() {
        check(plan == null)
        plan = checkNotNull(proposedPlan) { "O tutor ainda não propôs um plano" }
        proposedPlan = null
    }

    fun beginVerification(): String {
        check(currentGoal != null) { "Aceite um plano antes de verificar" }
        check(pendingRequestId == null) { "Verificação em andamento" }
        assessment = null
        return UUID.randomUUID().toString().also { pendingRequestId = it }
    }

    fun receive(event: SoftwareTutorEvent.Assessment): Boolean {
        if (pendingRequestId == null || event.requestId != pendingRequestId ||
            event.assessment.goalId != currentGoal?.id) return false
        assessment = event.assessment
        pendingRequestId = null
        return true
    }

    fun cancelVerification() { pendingRequestId = null }

    fun advance() {
        check(canAdvance) { "A etapa precisa de verificação correta antes de avançar" }
        index++
        assessment = null
    }
}

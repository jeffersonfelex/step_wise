package com.stepwise.software

import com.stepwise.core.GoalAssessment
import com.stepwise.core.TutoringGoal
import com.stepwise.core.TutoringPlan
import com.stepwise.engine.StepResult
import org.junit.Assert.*
import org.junit.Test

class CadProgressTest {
    private val samplePlan = TutoringPlan("support", listOf(TutoringGoal("sketch", "Desenhe o perfil")))

    @Test fun `model cannot advance or replace accepted plan`() {
        val progress = CadProgress()
        progress.propose(samplePlan)
        assertNull(progress.currentGoal)
        progress.acceptPlan()
        assertFalse(progress.propose(TutoringPlan("other", listOf(TutoringGoal("x", "Outra")))))
        assertFalse(progress.canAdvance)
        val request = progress.beginVerification()
        assertTrue(progress.receive(result(request, StepResult.CORRECT)))
        assertFalse(progress.completed)
        assertTrue(progress.canAdvance)
        progress.advance()
        assertTrue(progress.completed)
        assertFalse(progress.canAdvance)
    }

    @Test fun `rejects unsolicited mismatched and stale assessments`() {
        val progress = CadProgress().apply { propose(samplePlan); acceptPlan() }
        assertFalse(progress.receive(result("unrequested", StepResult.CORRECT)))
        val old = progress.beginVerification()
        progress.cancelVerification()
        val current = progress.beginVerification()
        assertFalse(progress.receive(result(old, StepResult.CORRECT)))
        assertFalse(progress.receive(SoftwareTutorEvent.Assessment(
            GoalAssessment("other-goal", StepResult.CORRECT, "Certo"), current)))
        assertTrue(progress.receive(result(current, StepResult.INCONCLUSIVE)))
        assertFalse(progress.canAdvance)
    }

    @Test fun `new verification invalidates previous approval`() {
        val progress = CadProgress().apply { propose(samplePlan); acceptPlan() }
        progress.receive(result(progress.beginVerification(), StepResult.CORRECT))
        progress.beginVerification()
        assertFalse(progress.canAdvance)
        progress.cancelVerification()
        assertFalse(progress.canAdvance)
    }

    private fun result(id: String, result: StepResult) =
        SoftwareTutorEvent.Assessment(GoalAssessment("sketch", result, "Feedback"), id)
}

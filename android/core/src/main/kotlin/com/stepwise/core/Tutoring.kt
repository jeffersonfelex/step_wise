package com.stepwise.core

import com.stepwise.engine.StepResult

enum class TutorMode { HARDWARE, SOFTWARE }

/** A goal is domain-neutral; visual classes and CAD operations belong to each mode. */
data class TutoringGoal(val id: String, val instruction: String) {
    init {
        require(id.isNotBlank())
        require(instruction.isNotBlank())
    }
}

data class TutoringPlan(val id: String, val goals: List<TutoringGoal>) {
    init {
        require(id.isNotBlank())
        require(goals.isNotEmpty())
        require(goals.map { it.id }.distinct().size == goals.size)
    }
}

/** Evidence from a verifier, not an instruction to advance the session automatically. */
data class GoalAssessment(
    val goalId: String,
    val result: StepResult,
    val feedback: String,
)

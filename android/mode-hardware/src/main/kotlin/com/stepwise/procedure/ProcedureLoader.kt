package com.stepwise.procedure

import android.content.Context
import org.json.JSONObject

/**
 * Carrega a definição de um procedimento a partir de
 * assets/procedures/<procedure_id>.json do módulo hardware (PROC no diagrama).
 */
class ProcedureLoader(private val context: Context) {

    fun load(procedureId: String): Procedure {
        require(procedureId.matches(Regex("[a-z0-9_]+"))) { "Procedimento inválido" }
        val json = context.assets.open("procedures/$procedureId.json").bufferedReader().use {
            JSONObject(it.readText())
        }
        val steps = json.getJSONArray("steps")
        require(steps.length() > 0) { "Procedimento sem etapas" }
        return Procedure(json.getString("procedure_id"), json.getString("title"),
            List(steps.length()) { index ->
                val step = steps.getJSONObject(index)
                val classes = step.getJSONArray("expected_classes")
                val instructions = step.getJSONObject("instructions")
                val errors = step.getJSONArray("known_errors")
                ProcedureStep(
                    stepId = step.getInt("step_id"),
                    name = step.getString("name"),
                    expectedClasses = List(classes.length()) { classes.getString(it) },
                    instructionsByLevel = instructions.keys().asSequence().associateWith { instructions.getString(it) },
                    knownErrors = List(errors.length()) {
                        val error = errors.getJSONObject(it)
                        KnownError(error.getString("error_id"), error.getString("message"))
                    }
                )
            })
    }
}

package com.stepwise.procedure

import android.content.Context
import java.io.IOException
import org.json.JSONObject

/**
 * Carrega a definição de um procedimento a partir de
 * assets/procedures/<procedure_id>.json (PROC no diagrama).
 *
 * Formato esperado (ver src/main/assets/procedures/montagem_engrenagens.json):
 * `procedure_id`, `title` e `steps[]`, cada etapa com `step_id`, `name`,
 * `expected_classes[]`, `instructions{L4..L1}` e `known_errors[]`
 * (`error_id`, `message`). O campo `fading_rule` do JSON é informativo -
 * a regra de fading em si vive em FadingPolicy, não é lida daqui.
 */
class ProcedureLoader(private val context: Context) {

    fun load(procedureId: String): Procedure {
        val json = try {
            context.assets.open("procedures/$procedureId.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        } catch (e: IOException) {
            throw IllegalArgumentException("Procedimento não encontrado: $procedureId", e)
        }

        val root = try {
            JSONObject(json)
        } catch (e: org.json.JSONException) {
            throw IllegalArgumentException("Procedimento inválido (JSON malformado): $procedureId", e)
        }

        val stepsJson = root.getJSONArray("steps")
        val steps = (0 until stepsJson.length()).map { i -> parseStep(stepsJson.getJSONObject(i)) }

        return Procedure(
            procedureId = root.getString("procedure_id"),
            title = root.getString("title"),
            steps = steps
        )
    }

    private fun parseStep(stepJson: JSONObject): ProcedureStep {
        val expectedClasses = stepJson.getJSONArray("expected_classes").let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        }

        val instructionsJson = stepJson.getJSONObject("instructions")
        val instructionsByLevel = instructionsJson.keys().asSequence()
            .associateWith { level -> instructionsJson.getString(level) }

        val knownErrorsJson = stepJson.optJSONArray("known_errors")
        val knownErrors = if (knownErrorsJson != null) {
            (0 until knownErrorsJson.length()).map { j ->
                val e = knownErrorsJson.getJSONObject(j)
                KnownError(errorId = e.getString("error_id"), message = e.getString("message"))
            }
        } else {
            emptyList()
        }

        return ProcedureStep(
            stepId = stepJson.getInt("step_id"),
            name = stepJson.getString("name"),
            expectedClasses = expectedClasses,
            instructionsByLevel = instructionsByLevel,
            knownErrors = knownErrors
        )
    }
}

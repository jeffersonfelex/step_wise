package com.stepwise.procedure

import android.content.Context
import java.io.IOException
import org.json.JSONException
import org.json.JSONObject

/**
 * Carrega a definição de um procedimento a partir de
 * assets/procedures/<procedure_id>.json do módulo hardware (PROC no diagrama).
 *
 * Formato esperado (ver src/main/assets/procedures/*.json):
 * `procedure_id`, `title` e `steps[]`, cada etapa com `step_id`, `name`,
 * `expected_classes[]`, `instructions{L4..L1}` e `known_errors[]`
 * (`error_id`, `message`). O campo `fading_rule` do JSON é informativo -
 * a regra de fading em si vive em FadingPolicy, não é lida daqui.
 */
class ProcedureLoader(private val context: Context) {

    fun load(procedureId: String): Procedure {
        require(procedureId.matches(Regex("[a-z0-9_]+"))) { "Procedimento inválido: $procedureId" }
        val text = try {
            context.assets.open("procedures/$procedureId.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        } catch (e: IOException) {
            throw IllegalArgumentException("Procedimento não encontrado: $procedureId", e)
        }
        return parse(text, procedureId)
    }

    internal companion object {
        /** Parsing puro (sem Context), testável em JVM. Aceita arquivos salvos com BOM UTF-8. */
        fun parse(text: String, procedureId: String): Procedure {
            val root = try {
                JSONObject(text.removePrefix("﻿"))
            } catch (e: JSONException) {
                throw IllegalArgumentException("Procedimento inválido (JSON malformado): $procedureId", e)
            }

            val stepsJson = root.getJSONArray("steps")
            require(stepsJson.length() > 0) { "Procedimento sem etapas: $procedureId" }
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
}

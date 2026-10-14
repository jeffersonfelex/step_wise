package com.stepwise.procedure

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Valida os procedimentos empacotados no APK sem precisar de aparelho. */
class ProcedureAssetsTest {

    private val assets = File("src/main/assets")

    private fun procedureFiles() = File(assets, "procedures").listFiles { f -> f.extension == "json" }.orEmpty().toList()

    @Test
    fun `todo procedimento carrega e o nome do arquivo e o procedure_id`() {
        val files = procedureFiles()
        assertTrue("nenhum procedimento encontrado em ${assets.absolutePath}", files.isNotEmpty())
        for (file in files) {
            val procedure = ProcedureLoader.parse(file.readText(Charsets.UTF_8), file.nameWithoutExtension)
            assertEquals(file.nameWithoutExtension, procedure.procedureId)
            assertEquals(procedure.steps.size, procedure.steps.map { it.stepId }.distinct().size)
            for (step in procedure.steps) {
                assertTrue("${file.name} etapa ${step.stepId} sem instrução L4", step.instructionsByLevel["L4"]?.isNotBlank() == true)
                assertTrue("${file.name} etapa ${step.stepId} sem classe esperada", step.expectedClasses.isNotEmpty())
            }
        }
    }

    @Test
    fun `classes e erros da torneira existem nos rotulos do classificador`() {
        val labelsJson = JSONObject(File(assets, "models/stepwise_real_classifier_v1_labels.json").readText(Charsets.UTF_8))
            .getJSONArray("labels")
        val labels = List(labelsJson.length()) { labelsJson.getString(it) }.toSet()
        val procedure = ProcedureLoader.parse(
            File(assets, "procedures/montagem_torneira_stepwise_v1.json").readText(Charsets.UTF_8),
            "montagem_torneira_stepwise_v1"
        )
        for (step in procedure.steps) {
            assertTrue("classe fora do modelo na etapa ${step.stepId}", labels.containsAll(step.expectedClasses))
            assertTrue("erro fora do modelo na etapa ${step.stepId}", labels.containsAll(step.knownErrors.map { it.errorId }))
        }
    }

    @Test
    fun `aceita arquivo salvo com BOM`() {
        val json = "﻿" + File(assets, "procedures/montagem_torneira_stepwise_v1.json").readText(Charsets.UTF_8)
        assertEquals(6, ProcedureLoader.parse(json, "montagem_torneira_stepwise_v1").steps.size)
    }
}

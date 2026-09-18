package com.stepwise.procedure

import android.content.Context

/**
 * Carrega a definição de um procedimento a partir de
 * app/src/main/assets/procedures/<procedure_id>.json (PROC no diagrama).
 */
class ProcedureLoader(private val context: Context) {

    fun load(procedureId: String): Procedure {
        // TODO: ler assets/procedures/$procedureId.json e desserializar para Procedure.
        throw NotImplementedError("Carregamento de procedimento pendente: $procedureId")
    }
}

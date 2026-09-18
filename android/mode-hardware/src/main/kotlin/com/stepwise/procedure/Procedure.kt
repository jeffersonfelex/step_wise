package com.stepwise.procedure

/** Um erro conhecido associado a uma etapa, com mensagem de feedback. */
data class KnownError(val errorId: String, val message: String)

/** Uma etapa do procedimento: classes esperadas, instruções por nível e erros conhecidos. */
data class ProcedureStep(
    val stepId: Int,
    val name: String,
    val expectedClasses: List<String>,
    val instructionsByLevel: Map<String, String>,
    val knownErrors: List<KnownError>
)

/** Definição completa de um procedimento (ex.: "Montagem de Engrenagens"). */
data class Procedure(
    val procedureId: String,
    val title: String,
    val steps: List<ProcedureStep>
)

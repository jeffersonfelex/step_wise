package com.stepwise.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Estado e eventos mínimos da sessão (DB no diagrama).
 * Guarda apenas o necessário para restaurar e auditar: etapa, nível,
 * último checkpoint, resultado e ajuda solicitada — nunca frames ou áudio.
 */
@Entity(tableName = "session_state")
data class SessionStateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val procedureId: String,
    val stepId: Int,
    val assistanceLevel: String,
    val lastResult: String,
    val helpRequested: Boolean,
    val timestampMillis: Long
)

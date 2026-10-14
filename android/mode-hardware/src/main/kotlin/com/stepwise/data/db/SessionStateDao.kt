package com.stepwise.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SessionStateDao {

    @Insert
    suspend fun insert(state: SessionStateEntity)

    @Query("SELECT * FROM session_state WHERE procedureId = :procedureId ORDER BY id DESC LIMIT 1")
    suspend fun latestCheckpoint(procedureId: String): SessionStateEntity?

    @Query("DELETE FROM session_state WHERE procedureId = :procedureId")
    suspend fun clear(procedureId: String)

    /**
     * Todo o histórico de desempenho salvo (todas as procedures), mais
     * recente primeiro. Usado pela tela de privacidade (LGPD) para o
     * aprendiz ver o que foi registrado antes de decidir apagar.
     */
    @Query("SELECT * FROM session_state ORDER BY id DESC")
    suspend fun listAll(): List<SessionStateEntity>

    /**
     * Apaga todo o histórico de desempenho, de qualquer procedimento.
     * Ação do aprendiz na tela de privacidade — ver docs/lgpd-privacidade.md
     * ("o aprendiz precisa poder ver e apagar o próprio histórico a
     * qualquer momento").
     */
    @Query("DELETE FROM session_state")
    suspend fun clearAll()
}

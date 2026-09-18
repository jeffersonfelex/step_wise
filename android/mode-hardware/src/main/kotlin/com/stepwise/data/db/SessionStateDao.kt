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
}

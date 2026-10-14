package com.stepwise.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Uses the production schema/DAO with a separate disposable database on the device. */
@RunWith(AndroidJUnit4::class)
class SessionStateDaoTest {
    private lateinit var context: Context
    private lateinit var databaseName: String
    private var database: AppDatabase? = null

    @Before
    fun createDatabase() {
        context = ApplicationProvider.getApplicationContext()
        databaseName = "stepwise-room-test-${UUID.randomUUID()}.db"
        database = openDatabase()
    }

    private fun openDatabase() = Room.databaseBuilder(
        context, AppDatabase::class.java, databaseName
    ).build()

    @After
    fun cleanUp() {
        try {
            database?.close()
        } finally {
            if (::databaseName.isInitialized) context.deleteDatabase(databaseName)
        }
    }

    private fun checkpoint(procedure: String, step: Int, timestamp: Long) = SessionStateEntity(
        procedureId = procedure,
        stepId = step,
        assistanceLevel = "L3",
        lastResult = "CORRECT",
        helpRequested = true,
        timestampMillis = timestamp,
    )

    @Test
    fun insertReadsEveryFieldAndPersistsAfterReopening() = runBlocking {
        val expected = checkpoint("test-gears", 2, 1_000L)
        val dao = requireNotNull(database).sessionStateDao()
        assertNull(dao.latestCheckpoint(expected.procedureId))
        dao.insert(expected)
        val saved = dao.latestCheckpoint(expected.procedureId)
        assertNotNull(saved)
        requireNotNull(saved)
        assertTrue("Room must generate a positive primary key", saved.id > 0)
        assertEquals(expected.copy(id = saved.id), saved)

        database?.close()
        database = openDatabase()
        assertEquals(saved, requireNotNull(database).sessionStateDao().latestCheckpoint(expected.procedureId))
    }

    @Test
    fun latestAndClearAreScopedToProcedure() = runBlocking {
        val dao = requireNotNull(database).sessionStateDao()
        val first = checkpoint("test-a", 1, 2_000L)
        // The current DAO defines latest by insertion ID, not by wall-clock timestamp.
        val latest = checkpoint("test-a", 2, 1_000L)
        val other = checkpoint("test-b", 4, 3_000L)
        dao.insert(first)
        dao.insert(latest)
        dao.insert(other)
        val savedLatest = requireNotNull(dao.latestCheckpoint("test-a"))
        val savedOther = requireNotNull(dao.latestCheckpoint("test-b"))
        assertEquals(latest.copy(id = savedLatest.id), savedLatest)
        assertEquals(other.copy(id = savedOther.id), savedOther)
        assertNull(dao.latestCheckpoint("test-missing"))

        dao.clear("test-a")
        assertNull(dao.latestCheckpoint("test-a"))
        assertEquals(savedOther, dao.latestCheckpoint("test-b"))
    }

    @Test
    fun listAllAndClearAllCoverEveryProcedure() = runBlocking {
        val dao = requireNotNull(database).sessionStateDao()
        dao.insert(checkpoint("test-a", 1, 1_000L))
        dao.insert(checkpoint("test-b", 2, 2_000L))
        assertEquals(listOf("test-b", "test-a"), dao.listAll().map { it.procedureId })

        dao.clearAll()
        assertTrue(dao.listAll().isEmpty())
    }
}

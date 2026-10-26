package com.davidsalama.lumiform.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidsalama.lumiform.data.local.dao.FormDao
import com.davidsalama.lumiform.data.local.dao.SelectionDao
import com.davidsalama.lumiform.data.mapper.toDomainTree
import com.davidsalama.lumiform.data.mapper.toEntities
import com.davidsalama.lumiform.domain.model.ChoiceQuestion
import com.davidsalama.lumiform.domain.model.FormNode
import com.davidsalama.lumiform.domain.model.Page
import com.davidsalama.lumiform.domain.model.Response
import com.davidsalama.lumiform.domain.model.ResponseSet
import com.davidsalama.lumiform.domain.model.TextQuestion
import com.davidsalama.lumiform.testutil.sampleFormTree
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the real SQL against an in-memory SQLite database on the JVM. */
@RunWith(AndroidJUnit4::class)
class FormDaoTest {

    private lateinit var database: FormDatabase
    private lateinit var formDao: FormDao
    private lateinit var selectionDao: SelectionDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), FormDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        formDao = database.formDao()
        selectionDao = database.selectionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun save(form: List<FormNode>, syncedAt: Long = 1_000) {
        val entities = form.toEntities()
        formDao.replaceForm(entities.nodes, entities.responses, syncedAt)
    }

    private suspend fun cachedForm() = formDao.observeForm().first().toDomainTree()

    private suspend fun selections() = selectionDao.observeSelections().first()
        .map { it.questionId to it.responseId }
        .toSet()

    /** A one-page form with a single choice question, for refresh scenarios. */
    private fun choiceForm(questionId: Long, vararg responseIds: Long) = listOf(
        Page(
            1, "P",
            listOf(
                ChoiceQuestion(
                    questionId, "Q",
                    ResponseSet(100, multipleSelection = true, responseIds.map { Response(it, "R$it", null) }),
                ),
            ),
        ),
    )

    @Test
    fun `saved form reads back as the same tree`() = runTest {
        save(sampleFormTree)

        assertEquals(sampleFormTree, cachedForm())
    }

    @Test
    fun `refresh replaces the whole tree, leaving no stale nodes or responses`() = runTest {
        save(sampleFormTree)
        val newForm = listOf(Page(1, "Only page", listOf(TextQuestion(2, "Only question"))))

        save(newForm)

        assertEquals(newForm, cachedForm())
        assertTrue(formDao.observeForm().first().all { it.responses.isEmpty() })
    }

    @Test
    fun `selections survive a refresh when their response still exists`() = runTest {
        save(sampleFormTree)
        selectionDao.replaceSelection(13, setOf(1012))
        selectionDao.replaceSelection(14, setOf(1021, 1023))

        save(sampleFormTree)

        assertEquals(setOf(13L to 1012L, 14L to 1021L, 14L to 1023L), selections())
    }

    @Test
    fun `refresh drops selections whose response was removed`() = runTest {
        save(choiceForm(questionId = 5, 51, 52))
        selectionDao.replaceSelection(5, setOf(51, 52))

        save(choiceForm(questionId = 5, 51))

        assertEquals(setOf(5L to 51L), selections())
    }

    @Test
    fun `refresh drops selections whose response moved to another question`() = runTest {
        save(choiceForm(questionId = 5, 51))
        selectionDao.replaceSelection(5, setOf(51))

        save(choiceForm(questionId = 6, 51))

        assertEquals(emptySet<Pair<Long, Long>>(), selections())
    }

    @Test
    fun `failed refresh rolls back and keeps the previous cache`() = runTest {
        save(sampleFormTree, syncedAt = 1_000)
        selectionDao.replaceSelection(13, setOf(1011))
        val duplicateIds = listOf(Page(1, "A", emptyList()), Page(1, "B", emptyList()))

        val error = runCatching { save(duplicateIds, syncedAt = 2_000) }.exceptionOrNull()

        assertTrue(error is SQLiteConstraintException)
        assertEquals(sampleFormTree, cachedForm())
        assertEquals(setOf(13L to 1011L), selections())
        assertEquals(1_000L, formDao.observeLastSyncedAt().first())
    }

    @Test
    fun `last synced time is stored with each refresh`() = runTest {
        assertEquals(null, formDao.observeLastSyncedAt().first())

        save(sampleFormTree, syncedAt = 1_000)
        save(sampleFormTree, syncedAt = 5_000)

        assertEquals(5_000L, formDao.observeLastSyncedAt().first())
    }

    @Test
    fun `replacing a selection only touches that question`() = runTest {
        save(sampleFormTree)
        selectionDao.replaceSelection(13, setOf(1011))
        selectionDao.replaceSelection(14, setOf(1021, 1022))

        selectionDao.replaceSelection(14, emptySet())
        selectionDao.replaceSelection(13, setOf(1012))

        assertEquals(setOf(13L to 1012L), selections())
        assertEquals(listOf(1012L), selectionDao.getSelection(13))
    }
}

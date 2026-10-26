package com.davidsalama.lumiform.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidsalama.lumiform.data.local.FormDatabase
import com.davidsalama.lumiform.domain.DataError
import com.davidsalama.lumiform.domain.RefreshResult
import com.davidsalama.lumiform.domain.model.ChoiceQuestion
import com.davidsalama.lumiform.domain.model.FormNode
import com.davidsalama.lumiform.domain.model.Page
import com.davidsalama.lumiform.domain.model.Section
import com.davidsalama.lumiform.domain.model.TextQuestion
import com.davidsalama.lumiform.testutil.FakeFormRemoteDataSource
import com.davidsalama.lumiform.testutil.sampleFormTree
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.UnknownHostException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Fake network, real (in-memory) database: the repository's job is mostly the interplay with
 * the cache, so that part runs the real SQL rather than a fake that re-implements it.
 */
@RunWith(AndroidJUnit4::class)
class OfflineFirstFormRepositoryTest {

    private val now = Instant.parse("2026-10-26T10:42:00Z")
    private val remote = FakeFormRemoteDataSource()
    private lateinit var database: FormDatabase
    private lateinit var repository: OfflineFirstFormRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), FormDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = OfflineFirstFormRepository(
            remote = remote,
            formDao = database.formDao(),
            selectionDao = database.selectionDao(),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private val singleChoice get() = choiceQuestion(13)
    private val multiChoice get() = choiceQuestion(14)

    private fun choiceQuestion(id: Long): ChoiceQuestion {
        fun find(nodes: List<FormNode>): ChoiceQuestion? = nodes.firstNotNullOfOrNull { node ->
            when (node) {
                is ChoiceQuestion -> node.takeIf { it.id == id }
                is Page -> find(node.children)
                is Section -> find(node.children)
                else -> null
            }
        }
        return checkNotNull(find(sampleFormTree))
    }

    @Test
    fun `nothing is cached before the first refresh`() = runTest {
        assertEquals(emptyList<FormNode>(), repository.observeForm().first())
        assertEquals(null, repository.observeLastSyncedAt().first())
    }

    @Test
    fun `successful refresh fills the cache and records the sync time`() = runTest {
        val result = repository.refresh()

        assertEquals(RefreshResult.Success, result)
        assertEquals(sampleFormTree, repository.observeForm().first())
        assertEquals(now, repository.observeLastSyncedAt().first())
    }

    @Test
    fun `failed first refresh reports the error and leaves the cache empty`() = runTest {
        remote.failWith(UnknownHostException())

        val result = repository.refresh()

        assertEquals(RefreshResult.Failed(DataError.NoConnection), result)
        assertEquals(emptyList<FormNode>(), repository.observeForm().first())
    }

    @Test
    fun `failed refresh keeps the previously cached form`() = runTest {
        repository.refresh()
        remote.failWith(UnknownHostException())

        val result = repository.refresh()

        assertEquals(RefreshResult.Failed(DataError.NoConnection), result)
        assertEquals(sampleFormTree, repository.observeForm().first())
        assertEquals(now, repository.observeLastSyncedAt().first())
    }

    @Test
    fun `a newer form from the server replaces the cached one`() = runTest {
        repository.refresh()
        val updated = listOf(Page(1, "Main Page", listOf(TextQuestion(3, "Updated text"))))
        remote.respondWith(updated)

        repository.refresh()

        assertEquals(updated, repository.observeForm().first())
    }

    @Test
    fun `cancellation is not swallowed as a failed refresh`() = runTest {
        remote.failWith(CancellationException("screen closed"))

        val thrown = runCatching { repository.refresh() }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
    }

    @Test
    fun `single choice - tap selects, another tap switches, same tap clears`() = runTest {
        repository.refresh()

        repository.toggleResponse(singleChoice, 1011)
        assertEquals(mapOf(13L to setOf(1011L)), repository.observeSelections().first())

        repository.toggleResponse(singleChoice, 1012)
        assertEquals(mapOf(13L to setOf(1012L)), repository.observeSelections().first())

        repository.toggleResponse(singleChoice, 1012)
        assertEquals(emptyMap<Long, Set<Long>>(), repository.observeSelections().first())
    }

    @Test
    fun `multiple choice - taps accumulate per question`() = runTest {
        repository.refresh()

        repository.toggleResponse(multiChoice, 1021)
        repository.toggleResponse(multiChoice, 1023)
        repository.toggleResponse(singleChoice, 1013)

        assertEquals(
            mapOf(13L to setOf(1013L), 14L to setOf(1021L, 1023L)),
            repository.observeSelections().first(),
        )
    }

    @Test
    fun `selections survive a refresh`() = runTest {
        repository.refresh()
        repository.toggleResponse(multiChoice, 1022)

        repository.refresh()

        assertEquals(mapOf(14L to setOf(1022L)), repository.observeSelections().first())
        assertEquals(2, remote.fetchCount)
    }
}

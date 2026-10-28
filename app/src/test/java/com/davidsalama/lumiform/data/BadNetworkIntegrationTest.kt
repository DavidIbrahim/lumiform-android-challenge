package com.davidsalama.lumiform.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.davidsalama.lumiform.data.local.FormDatabase
import com.davidsalama.lumiform.data.remote.FormApi
import com.davidsalama.lumiform.data.remote.RetrofitFormRemoteDataSource
import com.davidsalama.lumiform.data.remote.formApiHttpClient
import com.davidsalama.lumiform.data.remote.sharedHttpClient
import com.davidsalama.lumiform.domain.DataError
import com.davidsalama.lumiform.domain.RefreshResult
import com.davidsalama.lumiform.domain.model.FormNode
import com.davidsalama.lumiform.testutil.Fixtures
import com.davidsalama.lumiform.testutil.sampleFormTree
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.HttpUrl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A bad connection, end to end: a real socket (MockWebServer) through the production OkHttp,
 * Retrofit and retry setup into the repository and an in-memory Room database. Only the
 * timeouts are shorter than in the app, so a slow server costs milliseconds, not seconds.
 * The retry back-off runs on virtual time.
 */
@RunWith(AndroidJUnit4::class)
class BadNetworkIntegrationTest {

    private val now = Instant.parse("2026-10-28T10:42:00Z")
    private val server = MockWebServer()
    private lateinit var database: FormDatabase
    private lateinit var repository: OfflineFirstFormRepository

    @Before
    fun setUp() {
        server.start()
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), FormDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = repositoryFor(server.url("/"))
    }

    @After
    fun tearDown() {
        database.close()
        server.close()
    }

    private fun repositoryFor(baseUrl: HttpUrl): OfflineFirstFormRepository {
        val shared = sharedHttpClient(connectTimeout = 300.milliseconds, readTimeout = 300.milliseconds)
        val client = formApiHttpClient(shared, cache = null, callTimeout = 1.seconds)
        return OfflineFirstFormRepository(
            remote = RetrofitFormRemoteDataSource(FormApi.create(baseUrl, client, FormApi.json)),
            formDao = database.formDao(),
            selectionDao = database.selectionDao(),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )
    }

    private fun form() = MockResponse.Builder()
        .code(200)
        .addHeader("Content-Type", "text/plain; charset=utf-8")
        .body(Fixtures.sampleForm)

    private fun enqueue(vararg responses: MockResponse.Builder) = responses.forEach { server.enqueue(it.build()) }

    private suspend fun cachedForm(): List<FormNode> = repository.observeForm().first()

    /** Fills the cache with one good response, so the failure after it has something to keep. */
    private suspend fun syncOnce() {
        enqueue(form())
        assertEquals(RefreshResult.Success, repository.refresh())
    }

    @Test
    fun `a server that goes silent times out, is retried, and the cached form stays`() = runTest {
        syncOnce()
        repeat(3) { enqueue(form().onResponseStart(SocketEffect.Stall)) }

        val result = repository.refresh()

        assertEquals(RefreshResult.Failed(DataError.Timeout), result)
        assertEquals(1 + 3, server.requestCount)
        assertEquals(sampleFormTree, cachedForm())
    }

    @Test
    fun `a trickling download is cut off by the call timeout instead of loading forever`() = runTest {
        syncOnce()
        // A few bytes every 100 ms: no single read waits long enough to time out.
        repeat(3) { enqueue(form().throttleBody(64, 100, TimeUnit.MILLISECONDS)) }

        val result = repository.refresh()

        assertEquals(RefreshResult.Failed(DataError.Timeout), result)
        assertEquals(sampleFormTree, cachedForm())
    }

    @Test
    fun `a connection that drops mid-download is retried and the form still arrives`() = runTest {
        enqueue(form().onResponseBody(SocketEffect.CloseSocket()), form())

        val result = repository.refresh()

        assertEquals(RefreshResult.Success, result)
        assertEquals(2, server.requestCount)
        assertEquals(sampleFormTree, cachedForm())
        assertEquals(now, repository.observeLastSyncedAt().first())
    }

    @Test
    fun `a server error that recovers on the next attempt still loads the form`() = runTest {
        enqueue(MockResponse.Builder().code(503), form())

        val result = repository.refresh()

        assertEquals(RefreshResult.Success, result)
        assertEquals(2, server.requestCount)
        assertEquals(sampleFormTree, cachedForm())
    }

    @Test
    fun `a 404 is not retried and keeps the cached form`() = runTest {
        syncOnce()
        enqueue(MockResponse.Builder().code(404))

        val result = repository.refresh()

        assertEquals(RefreshResult.Failed(DataError.Server(404)), result)
        assertEquals(1 + 1, server.requestCount)
        assertEquals(sampleFormTree, cachedForm())
    }

    @Test
    fun `an unreachable server is reported as no connection`() = runTest {
        val unreachable = server.url("/")
        server.close()

        val result = repositoryFor(unreachable).refresh()

        assertEquals(RefreshResult.Failed(DataError.NoConnection), result)
        assertEquals(emptyList<FormNode>(), cachedForm())
    }
}

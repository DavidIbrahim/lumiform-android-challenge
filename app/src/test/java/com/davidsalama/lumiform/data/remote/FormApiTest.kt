package com.davidsalama.lumiform.data.remote

import com.davidsalama.lumiform.testutil.Fixtures
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

/** Runs the production Retrofit + converter setup against a local server. */
class FormApiTest {

    private val server = MockWebServer()
    private lateinit var api: FormApi

    @Before
    fun setUp() {
        server.start()
        api = FormApi.create(server.url("/"), OkHttpClient(), FormApi.json)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueue(code: Int, body: String, contentType: String = "text/plain; charset=utf-8") {
        server.enqueue(
            MockResponse.Builder()
                .code(code)
                .addHeader("Content-Type", contentType)
                .body(body)
                .build(),
        )
    }

    @Test
    fun `parses the body even though the gist serves it as text plain`() = runTest {
        enqueue(200, Fixtures.sampleForm)

        val form = api.getForm()

        assertEquals(listOf(1L, 10L), form.map { it.id })
    }

    @Test
    fun `requests the gist path`() = runTest {
        enqueue(200, "[]")

        api.getForm()

        val path = server.takeRequest().url.encodedPath
        assertEquals("/aruana-lumiform/383e1291df3e0cc1d49aeb14d45f5b94/raw/lumiform-android-test.json", path)
    }

    @Test
    fun `server error surfaces as HttpException with the status code`() = runTest {
        enqueue(500, "oops")

        val error = runCatching { api.getForm() }.exceptionOrNull()

        assertEquals(500, (error as HttpException).code())
    }

    @Test
    fun `malformed body surfaces as SerializationException`() = runTest {
        enqueue(200, """{"not": "a list"}""")

        val error = runCatching { api.getForm() }.exceptionOrNull()

        assertTrue(error is SerializationException)
    }
}

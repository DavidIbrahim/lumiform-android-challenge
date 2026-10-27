package com.davidsalama.lumiform.data

import com.davidsalama.lumiform.domain.DataError
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorMapperTest {

    @Test
    fun `offline and connection failures map to NoConnection`() {
        assertEquals(DataError.NoConnection, UnknownHostException("gist.githubusercontent.com").toDataError())
        assertEquals(DataError.NoConnection, ConnectException().toDataError())
        assertEquals(DataError.NoConnection, IOException("unexpected end of stream").toDataError())
    }

    @Test
    fun `timeouts map to Timeout`() {
        assertEquals(DataError.Timeout, SocketTimeoutException().toDataError())
        assertEquals(DataError.Timeout, InterruptedIOException("timeout").toDataError())
    }

    @Test
    fun `HTTP errors keep their status code`() {
        val error = HttpException(Response.error<Any>(503, "".toResponseBody()))

        assertEquals(DataError.Server(503), error.toDataError())
    }

    @Test
    fun `unreadable body maps to InvalidData`() {
        assertEquals(DataError.InvalidData, SerializationException("bad json").toDataError())
    }

    @Test
    fun `anything else maps to Unknown`() {
        assertEquals(DataError.Unknown, IllegalStateException().toDataError())
    }

    @Test
    fun `connection problems, timeouts, 5xx, 408 and 429 are worth retrying`() {
        val transient = listOf(
            DataError.NoConnection,
            DataError.Timeout,
            DataError.Server(500),
            DataError.Server(503),
            DataError.Server(408),
            DataError.Server(429),
        )
        transient.forEach { assertTrue("$it should be transient", it.isTransient()) }
    }

    @Test
    fun `client errors and unreadable data are not retried`() {
        val permanent = listOf(DataError.Server(404), DataError.Server(401), DataError.InvalidData, DataError.Unknown)
        permanent.forEach { assertFalse("$it should not be transient", it.isTransient()) }
    }
}

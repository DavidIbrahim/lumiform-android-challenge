package com.davidsalama.lumiform.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RetryWithBackoffTest {

    private class Transient : IOException()
    private class Permanent : IllegalStateException()

    private val retryTransient: (Throwable) -> Boolean = { it is Transient }

    @Test
    fun `success on the first attempt does not wait`() = runTest {
        var calls = 0

        val result = retryWithBackoff(shouldRetry = retryTransient) { calls++; "form" }

        assertEquals("form", result)
        assertEquals(1, calls)
        assertEquals(0L, currentTime)
    }

    @Test
    fun `transient failures are retried with doubling delays`() = runTest {
        var calls = 0

        val result = retryWithBackoff(shouldRetry = retryTransient) {
            calls++
            if (calls < 3) throw Transient()
            "form"
        }

        assertEquals("form", result)
        assertEquals(3, calls)
        // Waited 1 s, then 2 s (virtual time).
        assertEquals(3_000L, currentTime)
    }

    @Test
    fun `gives up after max attempts and rethrows the last failure`() = runTest {
        var calls = 0

        val error = runCatching {
            retryWithBackoff(maxAttempts = 3, shouldRetry = retryTransient) { calls++; throw Transient() }
        }.exceptionOrNull()

        assertTrue(error is Transient)
        assertEquals(3, calls)
    }

    @Test
    fun `permanent failure is not retried`() = runTest {
        var calls = 0

        val error = runCatching {
            retryWithBackoff(shouldRetry = retryTransient) { calls++; throw Permanent() }
        }.exceptionOrNull()

        assertTrue(error is Permanent)
        assertEquals(1, calls)
        assertEquals(0L, currentTime)
    }

    @Test
    fun `cancellation is never retried`() = runTest {
        var calls = 0

        val error = runCatching {
            retryWithBackoff(shouldRetry = { true }) { calls++; throw CancellationException() }
        }.exceptionOrNull()

        assertTrue(error is CancellationException)
        assertEquals(1, calls)
    }
}

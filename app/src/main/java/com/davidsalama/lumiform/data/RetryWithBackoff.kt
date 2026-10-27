package com.davidsalama.lumiform.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Runs [block], retrying up to [maxAttempts] times in total while [shouldRetry] says the failure
 * is worth retrying. The wait doubles each time (1 s, 2 s, ...), giving a flaky connection
 * time to recover without hammering it. The last failure is rethrown.
 */
suspend fun <T> retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelayMillis: Long = 1_000,
    factor: Double = 2.0,
    shouldRetry: (Throwable) -> Boolean,
    block: suspend () -> T,
): T {
    require(maxAttempts >= 1) { "maxAttempts must be at least 1" }
    var delayMillis = initialDelayMillis
    repeat(maxAttempts - 1) {
        try {
            return block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!shouldRetry(e)) throw e
        }
        delay(delayMillis)
        delayMillis = (delayMillis * factor).toLong()
    }
    return block()
}

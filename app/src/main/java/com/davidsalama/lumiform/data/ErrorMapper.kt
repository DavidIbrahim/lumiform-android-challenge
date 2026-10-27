package com.davidsalama.lumiform.data

import com.davidsalama.lumiform.domain.DataError
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException

fun Throwable.toDataError(): DataError = when (this) {
    is HttpException -> DataError.Server(code())
    // SocketTimeoutException and OkHttp's call timeout are both InterruptedIOExceptions.
    is InterruptedIOException -> DataError.Timeout
    is IOException -> DataError.NoConnection
    // Also covers kotlinx.serialization's MissingFieldException (a subclass).
    is SerializationException -> DataError.InvalidData
    else -> DataError.Unknown
}

/**
 * Worth retrying: the next attempt may succeed on its own. A 4xx (other than timeout and
 * rate limit) or a body we can't parse will fail the same way every time.
 */
fun DataError.isTransient(): Boolean = when (this) {
    DataError.NoConnection, DataError.Timeout -> true
    is DataError.Server -> code >= 500 || code == 408 || code == 429
    DataError.InvalidData, DataError.Unknown -> false
}

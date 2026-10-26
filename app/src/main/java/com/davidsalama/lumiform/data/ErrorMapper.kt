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

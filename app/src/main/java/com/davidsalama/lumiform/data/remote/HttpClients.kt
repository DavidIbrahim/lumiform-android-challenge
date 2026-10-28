package com.davidsalama.lumiform.data.remote

import okhttp3.Cache
import okhttp3.OkHttpClient
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * The client shared by the form API and image loading. Kept outside the DI module so tests can
 * build the same client, with shorter timeouts so they run fast.
 *
 * The read timeout is per read: it catches a server that goes silent, not one that trickles.
 */
fun sharedHttpClient(
    connectTimeout: Duration = 10.seconds,
    readTimeout: Duration = 15.seconds,
): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(connectTimeout.toJavaDuration())
    .readTimeout(readTimeout.toJavaDuration())
    .build()

/**
 * The form API's client: the shared one plus its own HTTP cache (for ETag revalidation) and a
 * limit on the whole request. On a connection that sends a few bytes every few seconds no
 * single read times out, so without [callTimeout] the form could stay loading forever.
 * Images don't get this limit: a large picture on a slow network may rightly take longer.
 */
fun formApiHttpClient(
    shared: OkHttpClient,
    cache: Cache?,
    callTimeout: Duration = 30.seconds,
): OkHttpClient = shared.newBuilder()
    .cache(cache)
    .callTimeout(callTimeout.toJavaDuration())
    .build()

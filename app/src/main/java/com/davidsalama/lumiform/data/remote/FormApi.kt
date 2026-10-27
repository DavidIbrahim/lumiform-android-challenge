package com.davidsalama.lumiform.data.remote

import com.davidsalama.lumiform.data.remote.dto.NodeDto
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers

interface FormApi {

    /**
     * `max-age=0` makes OkHttp treat its cached copy as stale, so it always asks the server, with
     * the cached ETag. An unchanged form comes back as an empty 304 and OkHttp serves the cached
     * body, which saves the download on a slow connection. Trusting the server's max-age=300
     * instead would let an offline refresh look successful and record a false sync time.
     * (`no-cache` doesn't work here: OkHttp then skips its cache entirely and sends no ETag.)
     */
    @Headers("Cache-Control: max-age=0")
    @GET("aruana-lumiform/383e1291df3e0cc1d49aeb14d45f5b94/raw/lumiform-android-test.json")
    suspend fun getForm(): List<NodeDto>

    companion object {
        const val BASE_URL = "https://gist.githubusercontent.com/"

        val json = Json {
            // New server fields must not break older app versions.
            ignoreUnknownKeys = true
            // An explicit null for a field with a default (e.g. "items": null) uses the default.
            coerceInputValues = true
        }

        /**
         * Builds the API. Kept outside the DI module so tests can point it at a MockWebServer
         * with the exact production converter setup.
         */
        fun create(baseUrl: HttpUrl, client: OkHttpClient, json: Json): FormApi =
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                // The gist is served as text/plain. The converter is registered for JSON
                // and Retrofit uses it for every body, so the response header is never checked.
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(FormApi::class.java)
    }
}

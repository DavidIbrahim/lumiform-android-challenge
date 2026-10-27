package com.davidsalama.lumiform.di

import com.davidsalama.lumiform.data.remote.FormApi
import com.davidsalama.lumiform.data.remote.FormRemoteDataSource
import com.davidsalama.lumiform.data.remote.RetrofitFormRemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import android.content.Context
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    abstract fun bindFormRemoteDataSource(impl: RetrofitFormRemoteDataSource): FormRemoteDataSource

    companion object {

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        /**
         * The API gets its own HTTP cache (for ETag revalidation) on top of the shared client, so
         * images aren't stored twice: Coil already caches them on disk.
         */
        @Provides
        @Singleton
        fun provideFormApi(client: OkHttpClient, @ApplicationContext context: Context): FormApi {
            val apiClient = client.newBuilder()
                .cache(Cache(File(context.cacheDir, "http_api"), maxSize = 5L * 1024 * 1024))
                .build()
            return FormApi.create(FormApi.BASE_URL.toHttpUrl(), apiClient, FormApi.json)
        }
    }
}

package com.davidsalama.lumiform.di

import com.davidsalama.lumiform.data.remote.FormApi
import com.davidsalama.lumiform.data.remote.FormRemoteDataSource
import com.davidsalama.lumiform.data.remote.RetrofitFormRemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
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

        @Provides
        @Singleton
        fun provideFormApi(client: OkHttpClient): FormApi =
            FormApi.create(FormApi.BASE_URL.toHttpUrl(), client, FormApi.json)
    }
}

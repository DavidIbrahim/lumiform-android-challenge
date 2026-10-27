package com.davidsalama.lumiform.testutil

import com.davidsalama.lumiform.di.ConnectivityModule
import com.davidsalama.lumiform.di.RepositoryModule
import com.davidsalama.lumiform.domain.ConnectivityObserver
import com.davidsalama.lumiform.domain.FormRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * For Hilt UI tests: the real app (activity, navigation, ViewModels, Compose) on top of an
 * in-memory repository that already has the sample form cached, and a device that's online.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [RepositoryModule::class, ConnectivityModule::class])
object TestDataModule {

    @Provides
    @Singleton
    fun provideFakeRepository(): FakeFormRepository = FakeFormRepository(cachedForm = sampleFormTree)

    @Provides
    fun provideRepository(fake: FakeFormRepository): FormRepository = fake

    @Provides
    @Singleton
    fun provideConnectivity(): ConnectivityObserver = FakeConnectivityObserver(online = true)
}

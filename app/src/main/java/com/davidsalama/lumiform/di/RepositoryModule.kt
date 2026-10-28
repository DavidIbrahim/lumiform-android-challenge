package com.davidsalama.lumiform.di

import com.davidsalama.lumiform.data.OfflineFirstFormRepository
import com.davidsalama.lumiform.domain.FormRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindFormRepository(impl: OfflineFirstFormRepository): FormRepository
}

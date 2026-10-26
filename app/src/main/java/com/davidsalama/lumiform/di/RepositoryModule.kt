package com.davidsalama.lumiform.di

import com.davidsalama.lumiform.data.OfflineFirstFormRepository
import com.davidsalama.lumiform.domain.FormRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindFormRepository(impl: OfflineFirstFormRepository): FormRepository

    companion object {
        // Injected so tests can pin "now".
        @Provides
        fun provideClock(): Clock = Clock.systemUTC()
    }
}

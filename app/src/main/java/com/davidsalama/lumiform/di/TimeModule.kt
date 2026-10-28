package com.davidsalama.lumiform.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

// Its own module: the repository and the ViewModel both need "now", and tests that replace the
// repository must still get a clock.
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    // Injected so tests can pin "now".
    @Provides
    fun provideClock(): Clock = Clock.systemUTC()
}

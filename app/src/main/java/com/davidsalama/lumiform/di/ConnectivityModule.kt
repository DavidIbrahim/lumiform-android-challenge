package com.davidsalama.lumiform.di

import com.davidsalama.lumiform.data.network.AndroidConnectivityObserver
import com.davidsalama.lumiform.domain.ConnectivityObserver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Its own module so UI tests can swap in a fake without replacing the whole network stack.
@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityModule {

    @Binds
    abstract fun bindConnectivityObserver(impl: AndroidConnectivityObserver): ConnectivityObserver
}

package com.davidsalama.lumiform.domain

import kotlinx.coroutines.flow.Flow

interface ConnectivityObserver {
    /** True while the device has a network that can reach the internet. Emits on every change. */
    val isOnline: Flow<Boolean>
}

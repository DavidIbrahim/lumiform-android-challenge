package com.davidsalama.lumiform.testutil

import com.davidsalama.lumiform.domain.ConnectivityObserver
import kotlinx.coroutines.flow.MutableStateFlow

class FakeConnectivityObserver(online: Boolean = true) : ConnectivityObserver {
    override val isOnline = MutableStateFlow(online)
}

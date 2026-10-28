package com.davidsalama.lumiform.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities

/**
 * The real observer against Robolectric's ConnectivityManager: what it reports at start, how it
 * follows the system's network callbacks, and that it stops listening when nobody collects.
 */
@RunWith(AndroidJUnit4::class)
class AndroidConnectivityObserverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val shadowConnectivity = shadowOf(connectivityManager)
    private val observer = AndroidConnectivityObserver(context)

    private val network = ShadowNetwork.newInstance(1)

    private fun capabilities(vararg capabilities: Int): NetworkCapabilities =
        ShadowNetworkCapabilities.newInstance().also { caps ->
            capabilities.forEach { shadowOf(caps).addCapability(it) }
        }

    private val validated get() = capabilities(NET_CAPABILITY_INTERNET, NET_CAPABILITY_VALIDATED)

    // Joined a Wi-Fi, but the system hasn't reached the internet through it (e.g. a hotel login page).
    private val captivePortal get() = capabilities(NET_CAPABILITY_INTERNET)

    private fun registeredCallback(): ConnectivityManager.NetworkCallback =
        shadowConnectivity.networkCallbacks.single()

    @Test
    fun `starts online when the active network has validated internet`() = runTest {
        shadowConnectivity.setNetworkCapabilities(connectivityManager.activeNetwork, validated)

        observer.isOnline.test {
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun `starts offline when there is no active network`() = runTest {
        shadowConnectivity.setActiveNetworkInfo(null)

        observer.isOnline.test {
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `a network behind a login page doesn't count as online`() = runTest {
        shadowConnectivity.setNetworkCapabilities(connectivityManager.activeNetwork, captivePortal)

        observer.isOnline.test {
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `follows the network as it is validated and then lost`() = runTest {
        shadowConnectivity.setActiveNetworkInfo(null)

        observer.isOnline.test {
            assertEquals(false, awaitItem())

            registeredCallback().onCapabilitiesChanged(network, captivePortal)
            // Still offline: distinctUntilChanged swallows the repeat, so nothing is emitted.
            registeredCallback().onCapabilitiesChanged(network, validated)
            assertEquals(true, awaitItem())

            registeredCallback().onLost(network)
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `listens only while collected`() = runTest {
        assertTrue(shadowConnectivity.networkCallbacks.isEmpty())

        observer.isOnline.test {
            awaitItem()
            assertEquals(1, shadowConnectivity.networkCallbacks.size)
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue(shadowConnectivity.networkCallbacks.isEmpty())
    }
}

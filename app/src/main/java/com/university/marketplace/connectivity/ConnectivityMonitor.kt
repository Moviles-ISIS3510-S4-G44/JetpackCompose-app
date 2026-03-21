package com.university.marketplace.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

interface ConnectivityMonitor {
    val isOnline: Flow<Boolean>

    fun isCurrentlyOnline(): Boolean
}

class AndroidConnectivityMonitor(
    context: Context
) : ConnectivityMonitor {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    override val isOnline: Flow<Boolean> = callbackFlow {
        val manager = connectivityManager
        if (manager == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(manager.isCurrentlyOnline())
            }

            override fun onLost(network: Network) {
                trySend(manager.isCurrentlyOnline())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                )
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        trySend(isCurrentlyOnline())

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            manager.registerNetworkCallback(request, callback)
        } catch (_: SecurityException) {
            trySend(false)
            close()
            return@callbackFlow
        }

        awaitClose {
            runCatching {
                manager.unregisterNetworkCallback(callback)
            }
        }
    }
        .distinctUntilChanged()
        .conflate()

    override fun isCurrentlyOnline(): Boolean = connectivityManager?.isCurrentlyOnline() == true
}

private fun ConnectivityManager.isCurrentlyOnline(): Boolean {
    val activeNetwork = activeNetwork ?: return false
    val capabilities = getNetworkCapabilities(activeNetwork) ?: return false

    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

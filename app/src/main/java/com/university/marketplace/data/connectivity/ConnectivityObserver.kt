package com.university.marketplace.data.connectivity

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

interface ConnectivityObserver {
	fun observe(): Flow<Boolean>
}

class AndroidConnectivityObserver(
	private val connectivityManager: ConnectivityManager
) : ConnectivityObserver {

	override fun observe(): Flow<Boolean> = callbackFlow {
		val callback = object : ConnectivityManager.NetworkCallback() {
			override fun onAvailable(network: Network) {
				trySend(true)
			}

			override fun onLost(network: Network) {
				trySend(isCurrentlyOnline())
			}

			override fun onUnavailable() {
				trySend(false)
			}
		}

		trySend(isCurrentlyOnline())

		val request = NetworkRequest.Builder()
			.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
			.build()

		connectivityManager.registerNetworkCallback(request, callback)

		awaitClose {
			connectivityManager.unregisterNetworkCallback(callback)
		}
	}.distinctUntilChanged()

	private fun isCurrentlyOnline(): Boolean {
		val network = connectivityManager.activeNetwork ?: return false
		val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
		return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
	}
}


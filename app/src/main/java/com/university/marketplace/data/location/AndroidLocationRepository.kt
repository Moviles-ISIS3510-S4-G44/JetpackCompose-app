package com.university.marketplace.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Suppress("unused")
class AndroidLocationRepository(
    context: Context
) : LocationRepository {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): UserLocation? {
        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        if (continuation.isActive) {
                            continuation.resume(UserLocation(location.latitude, location.longitude))
                        }
                    } else {
                        val cancellationTokenSource = CancellationTokenSource()
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            cancellationTokenSource.token
                        )
                            .addOnSuccessListener { currentLocation ->
                                val userLocation = currentLocation?.let {
                                    UserLocation(it.latitude, it.longitude)
                                }
                                if (continuation.isActive) {
                                    continuation.resume(userLocation)
                                }
                            }
                            .addOnFailureListener {
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                            }
                        continuation.invokeOnCancellation {
                            cancellationTokenSource.cancel()
                        }
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
        }
    }

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(): Flow<UserLocation> = callbackFlow {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(3000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(UserLocation(location.latitude, location.longitude))
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}


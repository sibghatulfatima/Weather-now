package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.model.CityLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentCityLocation(): CityLocation? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            return@withContext null
        }

        try {
            val cts = CancellationTokenSource()
            val location: Location? = suspendCancellableCoroutine<Location?> { continuation ->
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cts.token
                ).addOnSuccessListener { loc ->
                    if (loc != null) {
                        continuation.resume(loc)
                    } else {
                        // Fallback to last known location
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            continuation.resume(lastLoc)
                        }.addOnFailureListener {
                            continuation.resume(null)
                        }
                    }
                }.addOnFailureListener {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        continuation.resume(lastLoc)
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }
                }

                continuation.invokeOnCancellation {
                    cts.cancel()
                }
            }

            if (location == null) {
                return@withContext null
            }

            val lat = location.latitude
            val lon = location.longitude

            // Try reverse geocoding to obtain the user's city name
            var cityName = "Current Location"
            var country = ""
            var adminArea: String? = null

            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val addresses = suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lon, 1) { addrs ->
                            cont.resume(addrs)
                        }
                    }
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "My Location"
                        country = addr.countryName ?: ""
                        adminArea = addr.adminArea
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "My Location"
                        country = addr.countryName ?: ""
                        adminArea = addr.adminArea
                    }
                }
            } catch (e: Exception) {
                cityName = "My Location"
            }

            CityLocation(
                name = cityName,
                country = country,
                adminArea = adminArea,
                latitude = lat,
                longitude = lon,
                isCurrentGpsLocation = true
            )
        } catch (e: Exception) {
            null
        }
    }
}

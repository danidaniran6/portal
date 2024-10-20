package io.dee.portal.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val TAG = "LocationProvider"
    private val _locationFlow =
        MutableStateFlow<LocationProviderState?>(LocationProviderState.Loading)
    val locationFlow = _locationFlow.asStateFlow()

    private var fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest = LocationRequest().apply {
        interval = 2000
        fastestInterval = 1000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    private val locationSettingsRequest: LocationSettingsRequest = LocationSettingsRequest.Builder()
        .addLocationRequest(locationRequest)
        .build()

    init {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location: Location? = locationResult.lastLocation
                location?.let {
                    _locationFlow.value =
                        LocationProviderState.Success(io.dee.portal.core.data.local.Location(it))
                } ?: LocationProviderState.Error(NoSuchElementException())

            }
        }
    }


    @SuppressLint("MissingPermission")
    fun startLocationUpdatesInternal() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
            .addOnSuccessListener {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest, locationCallback, Looper.getMainLooper()
                )
                Log.d(TAG, "Location updates started!")
            }
            .addOnFailureListener {
                _locationFlow.value = LocationProviderState.Error(it)
            }
    }

    fun stopLocationUpdatesInternal() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
            .addOnSuccessListener {
                Log.d(TAG, "Location updates stopped!")
            }
            .addOnFailureListener {
                Log.d(TAG, "Location updates failed to stop!")
            }
        _locationFlow.value = null
    }


}

sealed interface LocationProviderState {
    data object Loading : LocationProviderState
    data class Success(val location: io.dee.portal.core.data.local.Location) : LocationProviderState
    data class Error(val exception: Exception) : LocationProviderState
}


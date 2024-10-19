package io.dee.portal.map_screen.data.datasource


import io.dee.portal.utils.LocationProvider
import io.dee.portal.utils.LocationProviderState
import kotlinx.coroutines.flow.StateFlow

interface LocationProviderDatasource {
    fun getLocationFlow(): StateFlow<LocationProviderState?>
    fun getLocation(): LocationProviderState
    fun startLocationUpdates()
    fun stopLocationUpdates()
}

class LocationProviderDatasourceImpl(
    private val locationProvider: LocationProvider
) :
    LocationProviderDatasource {
    override fun getLocationFlow(): StateFlow<LocationProviderState?> {
        return locationProvider.locationFlow
    }

    override fun startLocationUpdates() {
        locationProvider.startLocationUpdatesInternal()
    }

    override fun stopLocationUpdates() {
        locationProvider.stopLocationUpdatesInternal()
    }

    override fun getLocation(): LocationProviderState {
        return locationProvider.locationFlow.value ?: LocationProviderState.Loading
    }
}
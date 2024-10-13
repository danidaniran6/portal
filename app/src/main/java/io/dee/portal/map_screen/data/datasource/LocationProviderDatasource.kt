package io.dee.portal.map_screen.data.datasource


import io.dee.portal.utils.LocationProvider
import io.dee.portal.utils.LocationProviderState
import kotlinx.coroutines.flow.StateFlow

interface LocationProviderDatasource {
    fun getLocation(): StateFlow<LocationProviderState>
    fun startLocationUpdates()
    fun stopLocationUpdates()
}

class LocationProviderDatasourceImpl(
    private val locationProvider: LocationProvider
) :
    LocationProviderDatasource {
    override fun getLocation(): StateFlow<LocationProviderState> {
        return locationProvider.locationFlow
    }

    override fun startLocationUpdates() {
        locationProvider.startLocationUpdatesInternal()
    }

    override fun stopLocationUpdates() {
        locationProvider.stopLocationUpdatesInternal()
    }
}
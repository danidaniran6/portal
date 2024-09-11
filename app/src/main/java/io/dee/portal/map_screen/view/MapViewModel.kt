package io.dee.portal.map_screen.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dee.portal.data.local.Location
import io.dee.portal.map_screen.data.repository.MapRepository
import io.dee.portal.search_driver.data.dto.Driver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.neshan.common.model.LatLng
import org.neshan.mapsdk.model.Marker
import org.neshan.mapsdk.model.Polyline
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: MapRepository
) : ViewModel() {

    fun onEvent(event: MapEvents) {
        when (event) {
            is MapEvents.SetUserLocation -> updateUserLocation(event.location)
            is MapEvents.SetOriginLocation -> updateOriginLocation(event.location)
            is MapEvents.SetDestinationLocation -> updateDestinationLocation(event.location)
            is MapEvents.SetOriginDestinationLocation -> {
                updateOriginLocation(event.origin)
                updateDestinationLocation(event.destination)
            }

            MapEvents.ClearOriginLocation -> {
                updateOriginLocation(null)
            }

            MapEvents.ClearDestinationLocation -> {
                updateDestinationLocation(null)
            }

            is MapEvents.SetOriginMarker -> {
                updateOriginMarker(event.marker)
            }

            is MapEvents.SetDestinationMarker -> {
                updateDestinationMarker(event.marker)
            }

            is MapEvents.SetUserMarker -> {
                updateUserMarker(event.marker)
            }

            is MapEvents.SetOriginToDestinationLine -> {
                updateOriginToDestinationLine(event.line)
            }

            MapEvents.GetRoute -> {
                getRoute()
            }

            is MapEvents.SetRoutePolyline -> {
                updateRoutePolyline(event.line)
            }

            is MapEvents.UpdateDriver -> {
                updateDriver(event.driver)
            }

            is MapEvents.CancelRouting -> {
                updateOriginToDestinationLine(null)
                updateRoutePolyline(null)
                updateDestinationLocation(null)
            }
        }
    }

    private val _userMarker: MutableLiveData<Marker?> = MutableLiveData()
    var userMarker: LiveData<Marker?> = _userMarker
    private fun updateUserMarker(marker: Marker?) {
        _userMarker.value = marker
    }

    private val _originMarker: MutableLiveData<Marker?> = MutableLiveData()
    var originMarker: LiveData<Marker?> = _originMarker
    private fun updateOriginMarker(marker: Marker?) {
        _originMarker.value = marker
    }

    private val _destinationMarker: MutableLiveData<Marker?> = MutableLiveData()
    var destinationMarker: LiveData<Marker?> = _destinationMarker
    private fun updateDestinationMarker(marker: Marker?) {
        _destinationMarker.value = marker
    }

    private val _userLocation: MutableLiveData<Location?> = MutableLiveData()
    var userLocation: LiveData<Location?> = _userLocation

    private fun updateUserLocation(location: Location?) = viewModelScope.launch {
        withContext(Dispatchers.Main) {
            _userLocation.value = location
        }

    }

    private val _originLocation: MutableLiveData<Location?> = MutableLiveData()
    var originLocation: LiveData<Location?> = _originLocation
    private fun updateOriginLocation(location: Location?) = viewModelScope.launch {
        withContext(Dispatchers.Main) {
            _originLocation.value = location
        }
    }

    private val _destinationLocation: MutableLiveData<Location?> = MutableLiveData()
    var destinationLocation: LiveData<Location?> = _destinationLocation

    private fun updateDestinationLocation(location: Location?) = viewModelScope.launch {
        withContext(Dispatchers.Main) {
            _destinationLocation.value = location
        }
    }

    private val _originToDestinationLine: MutableLiveData<Polyline?> = MutableLiveData()
    var originToDestinationLine: LiveData<Polyline?> = _originToDestinationLine
    private fun updateOriginToDestinationLine(line: Polyline?) {
        _originToDestinationLine.value = line
    }

    private val _routePolyline: MutableLiveData<Polyline?> = MutableLiveData()
    var routePolyline: LiveData<Polyline?> = _routePolyline
    private fun updateRoutePolyline(line: Polyline?) {
        _routePolyline.value = line
    }

    private val _routingState: MutableLiveData<RoutingState> = MutableLiveData()
    var routingState: LiveData<RoutingState> = _routingState
    private fun getRoute() = viewModelScope.launch {
        _routingState.value = RoutingState.Loading
        val res = repository.getRoute(_originLocation.value!!, _destinationLocation.value!!)
        _routingState.value = res
    }

    private val _driver: MutableLiveData<Driver?> = MutableLiveData()
    val driver: LiveData<Driver?> = _driver
    private fun updateDriver(driver: Driver?) {
        _driver.value = driver
    }
}

sealed interface RoutingState {
    data object Loading : RoutingState
    data class Success(
        val routeOverviewPolylinePoints: ArrayList<LatLng>,
        val decodedSteps: ArrayList<List<LatLng>>
    ) : RoutingState

    data class Error(val message: String? = null) : RoutingState
}

sealed interface MapEvents {
    data class SetUserLocation(val location: Location?) : MapEvents
    data class SetOriginLocation(val location: Location?) : MapEvents
    data class SetDestinationLocation(val location: Location?) : MapEvents
    data class SetOriginDestinationLocation(val origin: Location?, val destination: Location?) :
        MapEvents

    data class SetOriginToDestinationLine(val line: Polyline?) : MapEvents

    data object ClearOriginLocation : MapEvents
    data object ClearDestinationLocation : MapEvents
    data class SetOriginMarker(val marker: Marker?) : MapEvents
    data class SetDestinationMarker(val marker: Marker?) : MapEvents
    data class SetUserMarker(val marker: Marker?) : MapEvents
    data object GetRoute : MapEvents
    data class SetRoutePolyline(val line: Polyline?) : MapEvents
    data class UpdateDriver(val driver: Driver?) : MapEvents
    data object CancelRouting : MapEvents

}
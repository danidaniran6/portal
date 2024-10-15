package io.dee.portal.map_screen.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dee.portal.core.data.local.Location
import io.dee.portal.map_screen.data.dto.DecodedSteps
import io.dee.portal.map_screen.data.repository.MapRepository
import io.dee.portal.utils.LocationProviderState
import io.dee.portal.utils.NavigationUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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
            is MapEvents.SetNavigatorLocation -> updateNavigatorLocation(event.location)

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

            is MapEvents.SetNavigatorMarker -> {
                updateNavigatorMarker(event.marker)
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


            is MapEvents.SetRoutCurrentStep -> {
                updateRoutCurrentStep(event.steps)
            }


            is MapEvents.SetRoutingOverView -> {
                updateRoutingOverView(event.overview)
            }

            is MapEvents.SetRoutingSteps -> {
                updateRoutingSteps(event.steps)
            }

            is MapEvents.CancelRouting -> {
                _startRouting.value = false
                updateNavigatorLocation(null)
                updateOriginLocation(null)
                updateOriginToDestinationLine(null)
                updateDestinationLocation(null)
                updateRoutingOverView(null)
                updateRoutingSteps(null)

            }

            MapEvents.StartLocationUpdates -> {
                repository.startLocationUpdates()
            }

            MapEvents.StopLocationUpdates -> {
                repository.stopLocationUpdates()
            }

            MapEvents.StartRouting -> {
                updateNavigatorLocation(_userLocation.value)
                startRouting()
            }

            MapEvents.FindCurrentStep -> {
                findCurrentStep()
            }

            MapEvents.SnapToLine -> {
                snapToLine()
            }
        }
    }

    private val _startRouting: MutableLiveData<Boolean> = MutableLiveData(false)
    var startRouting: LiveData<Boolean> = _startRouting
    private fun startRouting() {
        _startRouting.value = true
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
    val userLocation: LiveData<Location?> get() = _userLocation
    private fun updateUserLocation(location: Location?) {
        _userLocation.value = location
    }


    private val _navigatorLocation: MutableLiveData<Location?> = MutableLiveData()
    val navigatorLocation: LiveData<Location?> get() = _navigatorLocation
    private fun updateNavigatorLocation(location: Location?) {
        _navigatorLocation.value = location
    }

    private val _navigatorMarker: MutableLiveData<Marker?> = MutableLiveData()
    val navigatorMarker: LiveData<Marker?> get() = _navigatorMarker
    private fun updateNavigatorMarker(marker: Marker?) {
        _navigatorMarker.value = marker
    }

    val userLocationProvider: StateFlow<LocationProviderState?> =
        repository.getLocation().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

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

    private val _originToDestinationLine: MutableLiveData<Polyline?> = MutableLiveData(null)
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
        val res = repository.getRoute(_originLocation.value, _destinationLocation.value)
        _routingState.value = res
    }

    private val _routingOverView: MutableLiveData<List<LatLng>?> = MutableLiveData()
    var routingOverView: LiveData<List<LatLng>?> = _routingOverView
    private fun updateRoutingOverView(overview: List<LatLng>?) {
        _routingOverView.value = overview
    }

    private val _routingSteps: MutableLiveData<List<DecodedSteps>?> = MutableLiveData()
    var routingSteps: LiveData<List<DecodedSteps>?> = _routingSteps
    private fun updateRoutingSteps(steps: List<DecodedSteps>?) {
        _routingSteps.value = steps
    }

    private val _routCurrentStep: MutableLiveData<DecodedSteps?> = MutableLiveData()
    var routCurrentStep: LiveData<DecodedSteps?> = _routCurrentStep
    private fun updateRoutCurrentStep(steps: DecodedSteps?) {
        _routCurrentStep.value = steps
    }


    private fun findCurrentStep() = viewModelScope.launch {
        if (_navigatorLocation.value == null) return@launch
        if (_routingSteps.value == null) return@launch
        val currentStep = NavigationUtil.findCurrentStep(
            _navigatorLocation.value!!.getLatLng(),
            _routingSteps.value!!
        )
        _routingSteps.value?.getOrNull(currentStep)?.let {
            updateRoutCurrentStep(it)
        }
    }

    private fun snapToLine() = viewModelScope.launch {
        if (_navigatorLocation.value == null) return@launch
        if (_routCurrentStep.value?.decodedPolyline.isNullOrEmpty()) return@launch
        val snappedLocation = NavigationUtil.snapToLine(
            _navigatorLocation.value!!.getLatLng(),
            _routCurrentStep.value!!.decodedPolyline ?: emptyList()
        )
        updateNavigatorLocation(Location(snappedLocation))
    }

}

sealed interface RoutingState {
    data object Loading : RoutingState
    data class Success(
        val routeOverView: List<LatLng?>?, val routeSteps: List<DecodedSteps>
    ) : RoutingState

    data class Error(val message: String? = null) : RoutingState
}

sealed interface MapEvents {
    data class SetUserLocation(val location: Location?) : MapEvents
    data class SetOriginLocation(val location: Location?) : MapEvents
    data class SetDestinationLocation(val location: Location?) : MapEvents
    data class SetNavigatorLocation(val location: Location?) : MapEvents
    data class SetOriginDestinationLocation(val origin: Location?, val destination: Location?) :
        MapEvents

    data class SetOriginToDestinationLine(val line: Polyline?) : MapEvents

    data object ClearOriginLocation : MapEvents
    data object ClearDestinationLocation : MapEvents
    data class SetOriginMarker(val marker: Marker?) : MapEvents
    data class SetDestinationMarker(val marker: Marker?) : MapEvents
    data class SetUserMarker(val marker: Marker?) : MapEvents
    data class SetNavigatorMarker(val marker: Marker?) : MapEvents
    data object GetRoute : MapEvents
    data class SetRoutePolyline(val line: Polyline?) : MapEvents
    data class SetRoutingOverView(val overview: List<LatLng>?) : MapEvents
    data class SetRoutingSteps(val steps: List<DecodedSteps>?) : MapEvents
    data object CancelRouting : MapEvents
    data class SetRoutCurrentStep(val steps: DecodedSteps?) : MapEvents
    data object StartLocationUpdates : MapEvents
    data object StopLocationUpdates : MapEvents

    data object FindCurrentStep : MapEvents
    data object SnapToLine : MapEvents

    data object StartRouting : MapEvents


}
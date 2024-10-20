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
import io.dee.portal.utils.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                fetchRoute()
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
                _routingStatus.value = RoutingStatus.Cancel
                cancelRouting()
            }

            is MapEvents.InProgressRouting -> {
                _routingStatus.value = RoutingStatus.InProgress
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

    private val _routingStatus: MutableLiveData<RoutingStatus> = MutableLiveData()
    var routingStatus: LiveData<RoutingStatus> = _routingStatus
    private fun startRouting() {
        _routingStatus.value = RoutingStatus.Start
    }

    private fun cancelRouting() {
        updateNavigatorLocation(null)
        updateOriginLocation(null)
        updateOriginToDestinationLine(null)
        updateDestinationLocation(null)
        updateRoutingOverView(null)
        updateRoutingSteps(null)
        updateRoutCurrentStep(null)
        updateNextStep(null)
    }


    private val _userMarker: SingleLiveEvent<Marker?> = SingleLiveEvent()
    var userMarker: LiveData<Marker?> = _userMarker
    private fun updateUserMarker(marker: Marker?) {
        _userMarker.value = marker
    }

    private val _originMarker: SingleLiveEvent<Marker?> = SingleLiveEvent()
    var originMarker: LiveData<Marker?> = _originMarker
    private fun updateOriginMarker(marker: Marker?) {
        _originMarker.value = marker
    }

    private val _destinationMarker: SingleLiveEvent<Marker?> = SingleLiveEvent()
    var destinationMarker: LiveData<Marker?> = _destinationMarker
    private fun updateDestinationMarker(marker: Marker?) {
        _destinationMarker.value = marker
    }

    private val _userLocation: SingleLiveEvent<Location?> = SingleLiveEvent()
    val userLocation: LiveData<Location?> get() = _userLocation
    private fun updateUserLocation(location: Location?) {
        _userLocation.value = location
    }


    private val _navigatorLocation: MutableLiveData<Location?> = MutableLiveData()
    val navigatorLocation: LiveData<Location?> get() = _navigatorLocation
    private fun updateNavigatorLocation(location: Location?) {
        _navigatorLocation.value = location
    }

    private val _navigatorMarker: SingleLiveEvent<Marker?> = SingleLiveEvent()
    val navigatorMarker: LiveData<Marker?> get() = _navigatorMarker
    private fun updateNavigatorMarker(marker: Marker?) {
        _navigatorMarker.value = marker
    }

    val userLocationProvider: SharedFlow<LocationProviderState?> =
        repository.getLocationFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val _originLocation: SingleLiveEvent<Location?> = SingleLiveEvent()
    var originLocation: LiveData<Location?> = _originLocation
    private fun updateOriginLocation(location: Location?) = viewModelScope.launch {
        withContext(Dispatchers.Main) {
            _originLocation.value = location
        }
    }

    private val _destinationLocation: SingleLiveEvent<Location?> = SingleLiveEvent()
    var destinationLocation: LiveData<Location?> = _destinationLocation

    private fun updateDestinationLocation(location: Location?) = viewModelScope.launch {
        withContext(Dispatchers.Main) {
            _destinationLocation.value = location
        }
    }


    private val _originToDestinationLine: SingleLiveEvent<Polyline?> = SingleLiveEvent(null)
    var originToDestinationLine: LiveData<Polyline?> = _originToDestinationLine
    private fun updateOriginToDestinationLine(line: Polyline?) {
        _originToDestinationLine.value = line
    }

    private val _routePolyline: SingleLiveEvent<Polyline?> = SingleLiveEvent()
    var routePolyline: LiveData<Polyline?> = _routePolyline
    private fun updateRoutePolyline(line: Polyline?) {
        _routePolyline.value = line
    }


    private val _fetchRoutingState: MutableLiveData<FetchRoutingState> = MutableLiveData()
    var fetchRoutingState: LiveData<FetchRoutingState> = _fetchRoutingState
    private fun fetchRoute() = viewModelScope.launch {
        _fetchRoutingState.value = FetchRoutingState.Loading
        val res = repository.getRoute(_originLocation.value, _destinationLocation.value)
        _fetchRoutingState.value = res
    }

    private val _routingOverView: MutableLiveData<List<List<DecodedSteps>>?> = MutableLiveData()
    var routingOverView: LiveData<List<List<DecodedSteps>>?> = _routingOverView
    private fun updateRoutingOverView(overview: List<List<DecodedSteps>>?) {
        _routingOverView.value = overview
    }

    private val _routingSteps: MutableLiveData<List<DecodedSteps>?> = MutableLiveData()
    var routingSteps: LiveData<List<DecodedSteps>?> = _routingSteps
    private fun updateRoutingSteps(steps: List<DecodedSteps>?) {
        _routingSteps.value = steps
    }

    private val _routeCurrentStep: SingleLiveEvent<DecodedSteps?> = SingleLiveEvent()
    var routeCurrentStep: LiveData<DecodedSteps?> = _routeCurrentStep

    private fun updateRoutCurrentStep(steps: DecodedSteps?) {
        _routeCurrentStep.value = steps
    }

    private val _routeNextStep: SingleLiveEvent<DecodedSteps?> = SingleLiveEvent(null)
    var routeNextStep: LiveData<DecodedSteps?> = _routeNextStep
    private fun updateNextStep(steps: DecodedSteps?) {
        _routeNextStep.value = steps
    }

    private fun findCurrentStep() = viewModelScope.launch {
        if (_navigatorLocation.value == null) return@launch
        if (_routingSteps.value == null) return@launch
        val res = NavigationUtil.findCurrentStep(
            _navigatorLocation.value!!.getLatLng(), _routingSteps.value!!
        )
        when (res) {
            is NavigationUtil.CurrentStepState.StepFounded -> {
                val currentStep = _routingSteps.value?.getOrNull(res.stepIndex)
                    ?: _routingSteps.value?.firstOrNull()
                val nextStep = _routingSteps.value?.getOrNull(res.stepIndex + 1)
                updateRoutCurrentStep(currentStep)
                updateNextStep(nextStep)
            }

            is NavigationUtil.CurrentStepState.RouteFinished -> {
                _routingStatus.value = RoutingStatus.Finished
                cancelRouting()
            }

            is NavigationUtil.CurrentStepState.NeedReroute -> {
                _originLocation.value = _navigatorLocation.value
                fetchRoute()
            }

            else -> {}
        }

    }

    private fun snapToLine() = viewModelScope.launch {
        if (_navigatorLocation.value == null) return@launch
        if (_routeCurrentStep.value?.decodedPolyline.isNullOrEmpty()) return@launch
        val snappedLocation = NavigationUtil.snapToLine(
            _navigatorLocation.value!!.getLatLng(),
            _routeCurrentStep.value!!.decodedPolyline ?: emptyList()
        )

        updateNavigatorLocation(Location(snappedLocation))
    }

}

sealed interface FetchRoutingState {
    data object Loading : FetchRoutingState
    data class Success(
        val routeOverView: List<List<DecodedSteps>>, val routeSteps: List<DecodedSteps>
    ) : FetchRoutingState

    data class Error(val message: String? = null) : FetchRoutingState
}

sealed interface RoutingStatus {
    data object Start : RoutingStatus
    data object InProgress : RoutingStatus
    data object Finished : RoutingStatus
    data object Cancel : RoutingStatus
}


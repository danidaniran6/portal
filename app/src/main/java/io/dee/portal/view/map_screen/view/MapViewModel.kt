package io.dee.portal.view.map_screen.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dee.portal.data.local.Location
import io.dee.portal.view.map_screen.data.repository.MapRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.neshan.mapsdk.model.Marker
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: MapRepository
) : ViewModel() {

    private val _userMarker: MutableLiveData<Marker?> = MutableLiveData()
    var userMarker: LiveData<Marker?> = _userMarker
    fun updateUserMarker(marker: Marker?) {
        _userMarker.value = marker
    }

    private val _originMarker: MutableLiveData<Marker?> = MutableLiveData()
    var originMarker: LiveData<Marker?> = _originMarker
    fun updateOriginMarker(marker: Marker?) {
        _originMarker.value = marker
    }

    private val _destinationMarker: MutableLiveData<Marker?> = MutableLiveData()
    var destinationMarker: LiveData<Marker?> = _destinationMarker
    fun updateDestinationMarker(marker: Marker?) {
        _destinationMarker.value = marker
    }

    private val _userLocation: MutableLiveData<Location?> = MutableLiveData()
    var userLocation: LiveData<Location?> = _userLocation
    fun updateUserLocation(location: android.location.Location?) {
        _userLocation.value = location?.let { Location(it) }
    }
    fun updateUserLocation(location: Location?) {
        _userLocation.value = location
    }

    private val _originLocation: MutableLiveData<Location?> = MutableLiveData()
    var originLocation: LiveData<Location?> = _originLocation
    fun updateOriginLocation(location: Location?) {
        _originLocation.value = location
    }

    private val _destinationLocation: MutableLiveData<Location?> = MutableLiveData()
    var destinationLocation: LiveData<Location?> = _destinationLocation
    fun updateDestinationLocation(location: Location?) = viewModelScope.launch {
        withContext(Dispatchers.Main) {
            _destinationLocation.value = location

        }
    }
}
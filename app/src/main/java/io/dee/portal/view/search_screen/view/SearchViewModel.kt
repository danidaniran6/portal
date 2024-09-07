package io.dee.portal.view.search_screen.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dee.portal.data.local.Location
import io.dee.portal.data.local.LocationData
import io.dee.portal.view.search_screen.data.SearchRepository
import io.dee.portal.view.search_screen.data.SearchState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SearchRepository
) : ViewModel() {
    private val _searchState = MutableStateFlow(SearchState())
    val searchState = _searchState.asStateFlow()

    init {
        repository.getAllLocationsAsFlow().onEach {
            _searchState.value = _searchState.value.copy(
                searchedList = it.sortedByDescending { it.createdAt }.map {
                    Location().apply {
                        latitude = it.latitude
                        longitude = it.longitude
                        address = it.address
                        title = it.title
                        from = Location.Type.Local
                    }
                }
            )
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: SearchEvents) {
        when (event) {
            is SearchEvents.Search -> {
                search(event.term, event.lat, event.lng)
            }

            is SearchEvents.SaveLocation -> {
                saveLocation(event.location)
            }
        }
    }

    private fun saveLocation(location: Location) = viewModelScope.launch {
        val loc = LocationData().apply {
            latitude = location.latitude
            longitude = location.longitude
            address = location.address
            title = location.title
            createdAt = System.currentTimeMillis()
        }
        repository.insertLocation(loc)
    }


    private fun search(term: String, lat: Double, lng: Double) = viewModelScope.launch {
        _searchState.value = _searchState.value.copy(
            isLoading = true, term = term
        )
        val list = repository.search(term, lat, lng)?.map {
            Location(it)
        }?.filter { it.latitude != 0.0 && it.longitude != 0.0 }
        _searchState.value = _searchState.value.copy(
            isLoading = false, searchedList = list ?: emptyList()
        )
    }
}


sealed class SearchEvents {
    data class Search(val term: String, val lat: Double, val lng: Double) : SearchEvents()
    data class SaveLocation(val location: Location) : SearchEvents()
}
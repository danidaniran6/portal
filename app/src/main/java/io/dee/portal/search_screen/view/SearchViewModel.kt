package io.dee.portal.search_screen.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.dee.portal.core.data.db.entity.LocationData
import io.dee.portal.core.data.local.Location
import io.dee.portal.search_screen.data.repository.SearchRepository
import io.dee.portal.search_screen.data.SearchUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SearchRepository
) : ViewModel() {
    private val _searchState = MutableStateFlow<SearchUiState>(SearchUiState.Loading)
    val searchState = _searchState.asStateFlow()
    val localLocationsState =
        repository.getAllLocationsAsFlow().map<List<LocationData>, SearchUiState> { list ->
            val targetList = list.map {
                Location().apply {
                    latitude = it.latitude
                    longitude = it.longitude
                    address = it.address
                    title = it.title
                    from = Location.Type.Local
                }
            }
            SearchUiState.Success("", targetList)
        }.catch { SearchUiState.Error(it) }.stateIn(
            viewModelScope,
            kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            SearchUiState.Loading
        )

    val uiState = localLocationsState.combine(searchState) { local, search ->
        if (local is SearchUiState.Success && search is SearchUiState.Success) {
            val localList = local.searchedList
            val apiList = search.searchedList
            val term = search.term
            SearchUiState.Success(term, localList.filter {
                it.title.contains(term, true) ||
                        it.address.contains(term, true)
            }.plus(apiList))
        } else {
            local
        }
    }.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        SearchUiState.Loading
    )


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
        _searchState.emit(repository.search(term, lat, lng))
    }
}


sealed class SearchEvents {
    data class Search(val term: String, val lat: Double, val lng: Double) : SearchEvents()
    data class SaveLocation(val location: Location) : SearchEvents()
}
package io.dee.portal.search_screen.data.repository

import io.dee.portal.core.data.db.entity.LocationData
import io.dee.portal.search_screen.data.SearchUiState
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    suspend fun search(term: String, lat: Double, lng: Double): SearchUiState
    suspend fun insertLocation(locationData: LocationData)
    fun getAllLocationsAsFlow(): Flow<List<LocationData>>
}

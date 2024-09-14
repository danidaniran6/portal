package io.dee.portal.search_screen.data.datasource

import io.dee.portal.core.data.db.entity.LocationData
import kotlinx.coroutines.flow.Flow

interface SearchLocalDatasource {
    suspend fun insertLocation(locationData: LocationData)
    suspend fun getAllLocations(): List<LocationData>
    fun getAllLocationsAsFlow(): Flow<List<LocationData>>
}


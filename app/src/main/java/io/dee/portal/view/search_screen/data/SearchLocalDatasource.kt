package io.dee.portal.view.search_screen.data

import io.dee.portal.data.dao.LocationDataDao
import io.dee.portal.data.local.LocationData
import kotlinx.coroutines.flow.Flow

interface SearchLocalDatasource {
    suspend fun insertLocation(locationData: LocationData)
    suspend fun getAllLocations(): List<LocationData>
    fun getAllLocationsAsFlow(): Flow<List<LocationData>>
}

class SearchLocalDatasourceImpl(private val dao: LocationDataDao) : SearchLocalDatasource {
    override suspend fun insertLocation(locationData: LocationData) {
        dao.insertLocation(locationData)
    }

    override suspend fun getAllLocations(): List<LocationData> {
        return dao.getAllLocations()
    }

    override fun getAllLocationsAsFlow(): Flow<List<LocationData>> {
        return dao.getAllLocationsAsFlow()
    }
}
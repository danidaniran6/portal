package io.dee.portal.search_screen.data

import io.dee.portal.data.db.dao.LocationDataDao
import io.dee.portal.data.db.entity.LocationData
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
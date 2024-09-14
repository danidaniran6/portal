package io.dee.portal.search_screen.data.datasource

import io.dee.portal.core.data.db.dao.LocationDataDao
import io.dee.portal.core.data.db.entity.LocationData
import kotlinx.coroutines.flow.Flow

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
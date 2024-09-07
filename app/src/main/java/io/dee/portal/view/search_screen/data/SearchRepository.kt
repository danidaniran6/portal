package io.dee.portal.view.search_screen.data

import io.dee.portal.data.local.LocationData
import io.dee.portal.view.search_screen.data.dto.SearchDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface SearchRepository {
    suspend fun search(term: String, lat: Double, lng: Double): List<SearchDto.Item?>?
    suspend fun insertLocation(locationData: LocationData)
    fun getAllLocationsAsFlow(): Flow<List<LocationData>>
}

class SearchRepositoryImpl(
    private val remoteDatasource: SearchRemoteDatasource,
    private val localDatasource: SearchLocalDatasource
) : SearchRepository {
    override suspend fun search(term: String, lat: Double, lng: Double): List<SearchDto.Item?>? {
        return withContext(Dispatchers.IO) {
            val res = remoteDatasource.search(term, lat, lng)
            if (res.isSuccessful) {
                res.body()?.items
            } else {
                emptyList()
            }
        }
    }

    override suspend fun insertLocation(locationData: LocationData) {
        withContext(Dispatchers.IO) {
            localDatasource.insertLocation(locationData)
        }
    }

    override fun getAllLocationsAsFlow(): Flow<List<LocationData>> {
        return localDatasource.getAllLocationsAsFlow()
    }
}
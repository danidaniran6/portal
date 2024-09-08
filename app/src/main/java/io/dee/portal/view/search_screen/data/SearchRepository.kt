package io.dee.portal.view.search_screen.data

import io.dee.portal.data.db.entity.LocationData
import io.dee.portal.data.local.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface SearchRepository {
    suspend fun search(term: String, lat: Double, lng: Double): SearchUiState
    suspend fun insertLocation(locationData: LocationData)
    fun getAllLocationsAsFlow(): Flow<List<LocationData>>
}

class SearchRepositoryImpl(
    private val remoteDatasource: SearchRemoteDatasource,
    private val localDatasource: SearchLocalDatasource
) : SearchRepository {

    override suspend fun search(term: String, lat: Double, lng: Double): SearchUiState {
        return withContext(Dispatchers.IO) {
            val res = remoteDatasource.search(term, lat, lng)
            if (res.isSuccessful) {
                res.body()?.items?.map { Location(it) }
                    ?.filter { it.latitude != 0.0 && it.longitude != 0.0 }?.let {
                        SearchUiState.Success(
                            term, it
                        )
                    } ?: SearchUiState.Error(Throwable("Unknown Error"))
            } else {
                SearchUiState.Error(Throwable(res.errorBody()?.string() ?: "Unknown Error"))
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
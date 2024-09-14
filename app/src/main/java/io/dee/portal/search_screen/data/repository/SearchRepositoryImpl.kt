package io.dee.portal.search_screen.data.repository

import io.dee.portal.core.data.db.entity.LocationData
import io.dee.portal.core.data.local.Location
import io.dee.portal.search_screen.data.SearchUiState
import io.dee.portal.search_screen.data.datasource.SearchLocalDatasource
import io.dee.portal.search_screen.data.datasource.SearchRemoteDatasource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

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
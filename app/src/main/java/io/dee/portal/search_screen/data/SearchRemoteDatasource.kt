package io.dee.portal.search_screen.data

import io.dee.portal.api.PortalService
import io.dee.portal.search_screen.data.dto.SearchDto
import retrofit2.Response

interface SearchRemoteDatasource {
    suspend fun search(term: String, lat: Double, lng: Double): Response<SearchDto>
}

class SearchRemoteDatasourceImpl(private val service: PortalService) : SearchRemoteDatasource {
    override suspend fun search(term: String, lat: Double, lng: Double): Response<SearchDto> {
        return service.search(term, lat, lng)
    }
}
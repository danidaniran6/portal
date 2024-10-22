package io.dee.portal.search_screen.data.datasource

import io.dee.portal.core.data.api.PortalService
import io.dee.portal.search_screen.data.dto.SearchDto

class SearchRemoteDatasourceImpl(private val service: PortalService) : SearchRemoteDatasource {
    override suspend fun search(
        term: String,
        lat: Double,
        lng: Double
    ): retrofit2.Response<SearchDto> {
        return service.search(term, lat, lng)
    }
}
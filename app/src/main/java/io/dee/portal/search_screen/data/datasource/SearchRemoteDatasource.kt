package io.dee.portal.search_screen.data.datasource

import io.dee.portal.search_screen.data.dto.SearchDto
import retrofit2.Response

interface SearchRemoteDatasource {
    suspend fun search(term: String, lat: Double, lng: Double): Response<SearchDto>
}


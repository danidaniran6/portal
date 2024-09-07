package io.dee.portal.view.map_screen.data.datasource

import io.dee.portal.api.PortalService
import io.dee.portal.view.map_screen.data.dto.ReverseGeocodingResponse

interface ReverseGeoCodingDatasource {
    suspend fun reverseGeocoding(lat: Double, lng: Double): ReverseGeocodingResponse

}

class ReverseGeoCodingDatasourceImpl(private val portalService: PortalService) :
    ReverseGeoCodingDatasource {
    override suspend fun reverseGeocoding(lat: Double, lng: Double): ReverseGeocodingResponse {
        return portalService.reverseGeocoding(lat, lng)
    }
}
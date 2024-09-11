package io.dee.portal.map_screen.data.datasource

import io.dee.portal.api.PortalService
import io.dee.portal.data.local.Location
import io.dee.portal.map_screen.data.dto.RouteResponse
import retrofit2.Response


interface RoutingRemoteDatasource {
    suspend fun getRoute(origin: Location, destination: Location): Response<RouteResponse>
}

class RoutingRemoteDatasourceImpl(private val service: PortalService) : RoutingRemoteDatasource {
    override suspend fun getRoute(
        origin: Location,
        destination: Location
    ): Response<RouteResponse> {
        return service.fetchRouting(origin.getLatLngString(), destination.getLatLngString())
    }
}
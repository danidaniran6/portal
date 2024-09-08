package io.dee.portal.view.map_screen.data.repository

import io.dee.portal.data.local.Location
import io.dee.portal.view.map_screen.data.datasource.ReverseGeoCodingDatasource
import io.dee.portal.view.map_screen.data.datasource.RoutingRemoteDatasource
import io.dee.portal.view.map_screen.view.RoutingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.neshan.common.utils.PolylineEncoding

interface MapRepository {
    suspend fun reverseGeocoding(lat: Double, lng: Double): String
    suspend fun getRoute(origin: Location, destination: Location): RoutingState
}

class MapRepositoryImpl(
    private val datasource: ReverseGeoCodingDatasource,
    private val routingRemoteDatasource: RoutingRemoteDatasource
) : MapRepository {
    override suspend fun reverseGeocoding(lat: Double, lng: Double): String {
        return withContext(Dispatchers.IO) {
            val res = datasource.reverseGeocoding(lat, lng)
            if (res.status == "Ok") {
                res.formattedAddress ?: ""
            } else {
                ""
            }

        }
    }

    override suspend fun getRoute(origin: Location, destination: Location): RoutingState {
        return withContext(Dispatchers.IO) {
            val res = routingRemoteDatasource.getRoute(origin, destination)
            if (res.isSuccessful && res.body() != null) {
                val routes = res.body()!!.routes
                if (!routes.isNullOrEmpty()) {
                    val route = routes[0]
                    val routeOverviewPolylinePoints = route.overviewPolyline?.let {
                        PolylineEncoding.decode(
                            it.points
                        )
                    } ?: emptyList()
                    val decodedSteps = route.legs?.getOrNull(0)?.steps?.map {
                        PolylineEncoding.decode(it.polyline)
                    } ?: emptyList()
                    RoutingState.Success(routeOverviewPolylinePoints, decodedSteps)
                } else {
                    RoutingState.Error("Something went wrong")
                }
            } else {
                RoutingState.Error("Something went wrong")
            }
        }
    }
}
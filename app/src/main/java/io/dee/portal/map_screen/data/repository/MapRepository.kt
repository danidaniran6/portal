package io.dee.portal.map_screen.data.repository

import io.dee.portal.core.data.local.Location
import io.dee.portal.map_screen.data.datasource.LocationProviderDatasource
import io.dee.portal.map_screen.data.datasource.ReverseGeoCodingDatasource
import io.dee.portal.map_screen.data.datasource.RoutingRemoteDatasource
import io.dee.portal.map_screen.data.dto.DecodedSteps
import io.dee.portal.map_screen.view.RoutingState
import io.dee.portal.utils.LocationProviderState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.neshan.common.utils.PolylineEncoding

interface MapRepository {
    suspend fun reverseGeocoding(lat: Double, lng: Double): String
    suspend fun getRoute(origin: Location?, destination: Location?): RoutingState

    fun getLocation(): StateFlow<LocationProviderState>
    fun startLocationUpdates()
    fun stopLocationUpdates()
}

class MapRepositoryImpl(
    private val datasource: ReverseGeoCodingDatasource,
    private val routingRemoteDatasource: RoutingRemoteDatasource,
    private val locationProviderDatasource: LocationProviderDatasource
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

    override suspend fun getRoute(origin: Location?, destination: Location?): RoutingState {
        return withContext(Dispatchers.IO) {
            if (origin == null || destination == null) return@withContext RoutingState.Error("Origin or destination is not selected")
            try {
                val res = routingRemoteDatasource.getRoute(origin, destination)
                if (res.isSuccessful && res.body() != null) {
                    val routes = res.body()!!.routes
                    if (!routes.isNullOrEmpty()) {
                        val route = routes[0]
                        val overviewData = PolylineEncoding.decode(
                            route.overviewPolyline?.points ?: ""
                        )
                        val steps = route.legs?.getOrNull(0)?.steps
                        val decodedPolyline = steps?.map {
                            DecodedSteps(
                                it.name,
                                it.distance,
                                it.duration,
                                it.instruction,
                                it.modifier,
                                PolylineEncoding.decode(it.polyline ?: "")
                            )
                        }


                        RoutingState.Success(
                            overviewData,
                            decodedPolyline ?: emptyList()
                        )
                    } else {
                        RoutingState.Error("Something went wrong")
                    }
                } else {
                    RoutingState.Error("Something went wrong")
                }
            } catch (e: Exception) {
                RoutingState.Error(e.localizedMessage)
            }

        }
    }

    override fun getLocation(): StateFlow<LocationProviderState> {
        return locationProviderDatasource.getLocation()
    }

    override fun startLocationUpdates() {
        locationProviderDatasource.startLocationUpdates()
    }

    override fun stopLocationUpdates() {
        locationProviderDatasource.stopLocationUpdates()
    }


}
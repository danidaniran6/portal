package io.dee.portal.map_screen.data.repository

import io.dee.portal.core.data.local.Location
import io.dee.portal.map_screen.data.datasource.LocationProviderDatasource
import io.dee.portal.map_screen.data.datasource.ReverseGeoCodingDatasource
import io.dee.portal.map_screen.data.datasource.RoutingRemoteDatasource
import io.dee.portal.map_screen.data.dto.DecodedSteps
import io.dee.portal.map_screen.view.FetchRoutingState
import io.dee.portal.utils.LocationProviderState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.neshan.common.utils.PolylineEncoding

interface MapRepository {
    suspend fun reverseGeocoding(lat: Double, lng: Double): String
    suspend fun getRoute(origin: Location?, destination: Location?): FetchRoutingState

    fun getLocationFlow(): StateFlow<LocationProviderState?>
    fun getLocation(): LocationProviderState
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
            if (res.status.equals("OK", true)) {
                res.formattedAddress ?: ""
            } else {
                ""
            }

        }
    }

    override suspend fun getRoute(origin: Location?, destination: Location?): FetchRoutingState {
        return withContext(Dispatchers.IO) {
            if (origin == null || destination == null) return@withContext FetchRoutingState.Error("Origin or destination is not selected")
            try {
                val res = routingRemoteDatasource.getRoute(origin, destination)
                if (res.isSuccessful && res.body() != null) {
                    val routes = res.body()!!.routes
                    if (!routes.isNullOrEmpty()) {
                        val overviewData = withContext(Dispatchers.Default) {
                            routes.fold(mutableListOf<List<DecodedSteps>>()) { acc, route ->
                                val steps = route.legs?.getOrNull(0)?.steps
                                val decodedPolyline =
                                    steps?.map {
                                        DecodedSteps(
                                            it.name,
                                            it.distance,
                                            it.duration,
                                            it.instruction,
                                            it.modifier,
                                            PolylineEncoding.decode(it.polyline ?: "")
                                        )
                                    }
                                decodedPolyline?.let {
                                    acc.add(it)
                                }
                                return@fold acc
                            }
                        }
//                        val steps = firstRoute.legs?.getOrNull(0)?.steps
//                        val decodedPolyline = withContext(Dispatchers.Default) {
//                            steps?.map {
//                                DecodedSteps(
//                                    it.name,
//                                    it.distance,
//                                    it.duration,
//                                    it.instruction,
//                                    it.modifier,
//                                    PolylineEncoding.decode(it.polyline ?: "")
//                                )
//                            }
//                        }

                        FetchRoutingState.Success(
                            overviewData,
                            overviewData.firstOrNull() ?: emptyList()
                        )
                    } else {
                        FetchRoutingState.Error("Something went wrong")
                    }
                } else {
                    FetchRoutingState.Error("Something went wrong")
                }
            } catch (e: Exception) {
                FetchRoutingState.Error(e.localizedMessage)
            }

        }
    }

    override fun getLocationFlow(): StateFlow<LocationProviderState?> {
        return locationProviderDatasource.getLocationFlow()
    }

    override fun getLocation(): LocationProviderState {
        return locationProviderDatasource.getLocation()
    }

    override fun startLocationUpdates() {
        locationProviderDatasource.startLocationUpdates()
    }

    override fun stopLocationUpdates() {
        locationProviderDatasource.stopLocationUpdates()
    }


}
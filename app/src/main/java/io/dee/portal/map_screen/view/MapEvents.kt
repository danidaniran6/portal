package io.dee.portal.map_screen.view


import io.dee.portal.core.data.local.Location
import io.dee.portal.map_screen.data.dto.DecodedSteps
import org.neshan.mapsdk.model.Marker
import org.neshan.mapsdk.model.Polyline

sealed interface MapEvents {
    data class SetUserLocation(val location: Location?) : MapEvents
    data class SetOriginLocation(val location: Location?) : MapEvents
    data class SetDestinationLocation(val location: Location?) : MapEvents
    data class SetNavigatorLocation(val location: Location?) : MapEvents
    data class SetOriginDestinationLocation(val origin: Location?, val destination: Location?) :
        MapEvents

    data class SetOriginToDestinationLine(val line: Polyline?) : MapEvents

    data object ClearOriginLocation : MapEvents
    data object ClearDestinationLocation : MapEvents
    data class SetOriginMarker(val marker: Marker?) : MapEvents
    data class SetDestinationMarker(val marker: Marker?) : MapEvents
    data class SetUserMarker(val marker: Marker?) : MapEvents
    data class SetNavigatorMarker(val marker: Marker?) : MapEvents
    data object GetRoute : MapEvents
    data class SetRoutePolyline(val line: Polyline?) : MapEvents
    data class SetRoutingOverView(val overview: List<List<DecodedSteps>>) : MapEvents
    data class SetRoutingSteps(val steps: List<DecodedSteps>?) : MapEvents
    data object CancelRouting : MapEvents
    data class SetRoutCurrentStep(val steps: DecodedSteps?) : MapEvents
    data object StartLocationUpdates : MapEvents
    data object StopLocationUpdates : MapEvents

    data object FindCurrentStep : MapEvents
    data object SnapToLine : MapEvents

    data object StartRouting : MapEvents
    data object InProgressRouting : MapEvents


}
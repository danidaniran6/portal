package io.dee.portal.map_screen.view

import org.neshan.mapsdk.model.Marker

sealed interface MarkerEvents {
    data class AddMarker(val lat: Double, val lng: Double) : MarkerEvents
    data class RemoveMarker(val marker: Marker) : MarkerEvents
    data class MoveMarker(val newLat: Double, val newLng: Double) : MarkerEvents
}
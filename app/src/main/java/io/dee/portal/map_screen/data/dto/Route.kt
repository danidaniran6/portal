package io.dee.portal.map_screen.data.dto

import com.google.gson.annotations.SerializedName

data class Route(
    val legs: List<Leg>? = listOf(),
    @SerializedName("overview_polyline")
    val overviewPolyline: OverviewPolyline? = OverviewPolyline()
)
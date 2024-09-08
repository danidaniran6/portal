package io.dee.portal.view.map_screen.data.dto

import com.google.gson.annotations.SerializedName

data class Step(
    @SerializedName("bearing_after")
    val bearingAfter: Int? = 0,
    val distance: Distance? = Distance(),
    val duration: Duration? = Duration(),
    val instruction: String? = "",
    val modifier: String? = "",
    val name: String? = "",
    val polyline: String? = "",
    @SerializedName("start_location")
    val startLocation: List<Double>? = listOf(),
    val type: String? = ""
)
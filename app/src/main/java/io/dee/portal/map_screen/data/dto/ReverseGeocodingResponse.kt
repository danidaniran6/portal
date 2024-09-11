package io.dee.portal.map_screen.data.dto

import com.google.gson.annotations.SerializedName
import io.dee.portal.data.dto.GeneralResponse

data class ReverseGeocodingResponse(
    val city: String? = "",
    val district: String? = "",
    @SerializedName("formatted_address")
    val formattedAddress: String? = "",
    @SerializedName("in_odd_even_zone")
    val inOddEvenZone: String? = "",
    @SerializedName("in_traffic_zone")
    val inTrafficZone: String? = "",
    @SerializedName("municipality_zone")
    val municipalityZone: String? = "",
    val neighbourhood: String? = "",
    val place: Any? = Any(),
    @SerializedName("route_name")
    val routeName: String? = "",
    @SerializedName("route_type")
    val routeType: String? = "",
    val state: String? = "",
    val village: Any? = Any()
) : GeneralResponse()
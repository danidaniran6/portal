package io.dee.portal.map_screen.data.dto

import com.google.gson.annotations.SerializedName
import io.dee.portal.R
import org.neshan.common.model.LatLng

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

data class DecodedSteps(
    val name: String?,
    val distance: Distance? = Distance(),
    val duration: Duration? = Duration(),
    val instruction: String? = "",
    val modifier: String? = "",
    val decodedPolyline: List<LatLng>? = listOf(),
) {
    enum class ModifierEnum(val value: String, val icon: Int) {
        RIGHT("right", R.drawable.ic_arrow_right),
        SLIGHT_RIGHT("slight_right", R.drawable.ic_arrow_right),
        SHARP_RIGHT("sharp_right", R.drawable.ic_arrow_right),
        LEFT("left", R.drawable.ic_arrow_left),
        SLIGHT_LEFT("slight_left", R.drawable.ic_arrow_left),
        SHARP_LEFT("sharp_left", R.drawable.ic_arrow_left),
        U_TURN("u_turn", R.drawable.ic_arrow_down),
        STRAIGHT("straight", R.drawable.ic_arrow_up);

        companion object {
            fun getModifier(modifier: String?): ModifierEnum {
                return entries.find { it.value == modifier } ?: ModifierEnum.STRAIGHT
            }
        }
    }
}
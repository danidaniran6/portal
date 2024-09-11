package io.dee.portal.map_screen.data.dto

data class Leg(
    val distance: Distance? = Distance(),
    val duration: Duration? = Duration(),
    val steps: List<Step>? = listOf(),
    val summary: String? = ""
)
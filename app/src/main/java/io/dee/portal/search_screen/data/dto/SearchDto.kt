package io.dee.portal.search_screen.data.dto

data class SearchDto(
    val count: Int? = 0,
    val items: List<Item?>? = listOf()
) {
    data class Item(
        val address: String? = "",
        val category: String? = "",
        val location: Location? = Location(),
        val neighbourhood: String? = "",
        val region: String? = "",
        val title: String? = "",
        val type: String? = ""
    ) {
        data class Location(
            val x: Double? = 0.0,
            val y: Double? = 0.0,
            val z: String? = ""
        )
    }
}
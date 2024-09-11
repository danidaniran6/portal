package io.dee.portal.view.search_driver.data.dto

data class Driver(
    val name: String,
    val phoneNumber: String,
    val car: Car
) {
    data class Car(
        val model: String,
        val color: String,
        val plateNumber: Plate
    ) {
        fun getCarName(): String {
            return "$model $color"
        }

        data class Plate(
            val left: String,
            val letter: String,
            val right: String,
            val code: String
        ) {
            fun getPlateNumber(): String {
                return "$right $letter $left"
            }
        }
    }


}
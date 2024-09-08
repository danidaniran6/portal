package io.dee.portal.view.search_driver.data.dto

data class Driver(
    val name: String,
    val phoneNumber: String,
    val car: Car
) {
    data class Car(
        val model: String,
        val color: String,
        val plateNumber: String
    )


}
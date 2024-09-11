package io.dee.portal.search_driver.data

import io.dee.portal.search_driver.data.dto.Driver
import retrofit2.Response


interface SearchDriverRemoteDatasource {
    suspend fun getDriver(): Response<Driver>
}

class SearchDriverRemoteDatasourceImpl :
    SearchDriverRemoteDatasource {
    override suspend fun getDriver(): Response<Driver> {
        val mockDriver = Driver(
            "آقای راننده محترم",
            "1234567890",
            Driver.Car("پراید", "قرمز", Driver.Car.Plate("12", "ب", "345", "12"))
        )
        return Response.success(mockDriver)
    }
}
package io.dee.portal.view.search_driver.data

import io.dee.portal.view.search_driver.data.dto.Driver
import io.dee.portal.view.search_driver.data.dto.Driver.Car
import retrofit2.Response


interface SearchDriverRemoteDatasource {
    suspend fun getDriver(): Response<Driver>
}

class SearchDriverRemoteDatasourceImpl() :
    SearchDriverRemoteDatasource {
    override suspend fun getDriver(): Response<Driver> {
        val mockDriver = Driver(
            "آقای راننده محترم",
            "1234567890",
            Car("پراید", "قرمز", Car.Plate("12", "ب", "345", "12"))
        )
        return Response.success(mockDriver)
    }
}
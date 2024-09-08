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
            "John Doe",
            "1234567890",
            Car("Toyota", "Red", "ABC123")
        )
        return Response.success(mockDriver)
    }
}
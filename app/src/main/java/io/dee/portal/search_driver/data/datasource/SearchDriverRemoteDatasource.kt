package io.dee.portal.search_driver.data.datasource

import io.dee.portal.search_driver.data.dto.Driver
import retrofit2.Response


interface SearchDriverRemoteDatasource {
    suspend fun getDriver(): Response<Driver>
}


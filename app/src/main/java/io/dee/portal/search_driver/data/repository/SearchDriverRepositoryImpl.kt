package io.dee.portal.search_driver.data.repository

import io.dee.portal.search_driver.data.datasource.SearchDriverRemoteDatasource
import io.dee.portal.search_driver.view.SearchDriverState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class SearchDriverRepositoryImpl(
    private val remoteDatasource: SearchDriverRemoteDatasource
) : SearchDriverRepository {
    override suspend fun getDriver(): SearchDriverState {
        return withContext(Dispatchers.IO) {
            delay(3000)
            val res = remoteDatasource.getDriver()
            try {
                if (res.isSuccessful) {
                    SearchDriverState.Success(res.body()!!)
                } else {
                    SearchDriverState.Error("Error")
                }
            } catch (e: Exception) {
                SearchDriverState.Error("Error")
            }
        }

    }
}
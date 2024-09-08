package io.dee.portal.view.search_driver.data

import io.dee.portal.view.search_driver.view.SearchDriverState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

interface SearchDriverRepository {
    suspend fun getDriver(): SearchDriverState
}

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
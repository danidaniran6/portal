package io.dee.portal.search_driver.data.repository

import io.dee.portal.search_driver.view.SearchDriverState

interface SearchDriverRepository {
    suspend fun getDriver(): SearchDriverState
}


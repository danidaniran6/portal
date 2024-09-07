package io.dee.portal.view.map_screen.data.repository

import io.dee.portal.view.map_screen.data.datasource.ReverseGeoCodingDatasource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface MapRepository {
    suspend fun reverseGeocoding(lat: Double, lng: Double): String
}

class MapRepositoryImpl(private val datasource: ReverseGeoCodingDatasource) : MapRepository {
    override suspend fun reverseGeocoding(lat: Double, lng: Double): String {
        return withContext(Dispatchers.IO) {
            val res = datasource.reverseGeocoding(lat, lng)
            if (res.status == "Ok") {
                res.formattedAddress ?: ""
            } else {
                ""
            }

        }
    }
}
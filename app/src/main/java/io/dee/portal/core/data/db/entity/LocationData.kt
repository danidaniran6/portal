package io.dee.portal.core.data.db.entity

import androidx.room.Entity

@Entity(tableName = "locations", primaryKeys = ["latitude", "longitude"])
data class LocationData(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var address: String = "",
    var title: String = "",
    var createdAt: Long = System.currentTimeMillis()
)
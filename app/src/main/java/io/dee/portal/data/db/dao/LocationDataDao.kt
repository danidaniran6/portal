package io.dee.portal.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.dee.portal.data.db.entity.LocationData
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDataDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocation(locationData: LocationData)

    @Query("SELECT * FROM locations")
    suspend fun getAllLocations(): List<LocationData>

    @Query("SELECT * FROM locations")
    fun getAllLocationsAsFlow(): Flow<List<LocationData>>
}
package io.dee.portal.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.dee.portal.core.data.db.dao.LocationDataDao
import io.dee.portal.core.data.db.entity.LocationData
import io.dee.portal.utils.DATABASE_NAME

@Database(entities = [LocationData::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDataDao(): LocationDataDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME).build()
        }
    }
}
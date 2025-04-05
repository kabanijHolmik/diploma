package com.example.ogz_pgz.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.ogz_pgz.dao.CalculationDao
import com.example.ogz_pgz.dao.CoordinateDao
import com.example.ogz_pgz.dao.GeodesicParametersDao
import com.example.ogz_pgz.entities.CalculationEntity
import com.example.ogz_pgz.entities.CoordinateEntity
import com.example.ogz_pgz.entities.GeodesicParametersEntity

@Database(
    entities = [
        CoordinateEntity::class,
        GeodesicParametersEntity::class,
        CalculationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coordinateDao(): CoordinateDao
    abstract fun parametersDao(): GeodesicParametersDao
    abstract fun calculationDao(): CalculationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "geodesic_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
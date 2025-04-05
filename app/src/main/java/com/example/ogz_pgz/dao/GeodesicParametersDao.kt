package com.example.ogz_pgz.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ogz_pgz.entities.GeodesicParametersEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeodesicParametersDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(parameters: GeodesicParametersEntity): Long

    @Query("SELECT * FROM geodesic_parameters WHERE distance = :distance AND azimuth = :azimuth AND elevationAngle = :elevationAngle AND distanceIncline = :distanceIncline LIMIT 1")
    suspend fun getParameters(distance: Double, azimuth: Double, elevationAngle: Double, distanceIncline: Double): GeodesicParametersEntity?

    @Query("SELECT * FROM geodesic_parameters WHERE id = :id")
    suspend fun getParametersById(id: Long): GeodesicParametersEntity?

    @Query("SELECT * FROM geodesic_parameters")
    fun getAllParameters(): Flow<List<GeodesicParametersEntity>>
}

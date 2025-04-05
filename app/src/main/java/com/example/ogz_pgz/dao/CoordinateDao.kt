package com.example.ogz_pgz.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ogz_pgz.entities.CoordinateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoordinateDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(coordinate: CoordinateEntity): Long

    @Query("SELECT * FROM coordinates WHERE latitude = :latitude AND longitude = :longitude AND altitude = :altitude LIMIT 1")
    suspend fun getCoordinate(latitude: Double, longitude: Double, altitude: Double): CoordinateEntity?

    @Query("SELECT * FROM coordinates WHERE id = :id")
    suspend fun getCoordinateById(id: Long): CoordinateEntity?

    @Query("SELECT * FROM coordinates")
    fun getAllCoordinates(): Flow<List<CoordinateEntity>>
}
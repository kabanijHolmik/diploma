package com.example.ogz_pgz.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geodesic_parameters")
data class GeodesicParametersEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val distance: Double,
    val azimuth: Double,
    val elevationAngle: Double,
    val distanceIncline: Double
)
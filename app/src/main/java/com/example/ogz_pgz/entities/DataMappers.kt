package com.example.ogz_pgz.entities

import model.Coordinate
import model.GeodesicParameters

fun Coordinate.toEntity(): CoordinateEntity =
    CoordinateEntity(
        id = id,
        latitude = latitude,
        longitude = longitude,
        altitude = altitude
    )

fun GeodesicParameters.toEntity(): GeodesicParametersEntity =
    GeodesicParametersEntity(
        id = id,
        distance = distance,
        azimuth = azimuth,
        elevationAngle = elevationAngle,
        distanceIncline = distanceIncline
    )

fun CoordinateEntity.toDomain(): Coordinate =
    Coordinate(
        id = id,
        latitude = latitude,
        longitude = longitude,
        altitude = altitude
    )

fun GeodesicParametersEntity.toDomain(): GeodesicParameters =
    GeodesicParameters(
        id = id,
        distance = distance,
        azimuth = azimuth,
        elevationAngle = elevationAngle,
        distanceIncline = distanceIncline
    )
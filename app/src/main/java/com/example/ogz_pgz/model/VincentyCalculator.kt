package com.example.ogz_pgz.model

import model.Coordinate
import model.GeodesicParameters

class VincentyCalculator : GeodesicCalculator {

    override fun calculateOGZ(start: Coordinate, end: Coordinate): GeodesicParameters {
        val (distance, azimuth) = Vincenty.inverseProblem(
            start.latitude, start.longitude, end.latitude, end.longitude
        )

        // Вычисление угла возвышения и наклонного расстояния
        val elevationAngle = GeodesicMath.calculateElevationAngle(start, end, distance)
        val distanceIncline = GeodesicMath.calculateInclinedDistance(distance, start, end)

        return GeodesicParameters(
            distance = distance,
            azimuth = azimuth,
            elevationAngle = elevationAngle,
            distanceIncline = distanceIncline
        )
    }

    override fun calculatePGZ(start: Coordinate, params: GeodesicParameters): Coordinate {
        // Вычисление координат конечной точки по методу Винсента
        val (latitude, longitude) = Vincenty.directProblem(
            start.latitude, start.longitude, params.azimuth, params.distance
        )

        // Вычисление высоты конечной точки
        val heightDifference = params.distance * Math.tan(GeodesicMath.toRadians(params.elevationAngle))
        val altitude = start.altitude + heightDifference

        return Coordinate(
            latitude = latitude,
            longitude = longitude,
            altitude = altitude
        )
    }
}
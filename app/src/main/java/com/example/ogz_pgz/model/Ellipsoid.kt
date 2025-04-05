package model

data class Ellipsoid(
    val majorAxis: Double,
    val firstEccentricity: Double,
    val secondEccentricity: Double,
    val minorAxis: Double
)
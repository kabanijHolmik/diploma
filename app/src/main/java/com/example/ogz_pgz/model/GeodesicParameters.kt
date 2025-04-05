package model

data class GeodesicParameters(
    val id: Long = 0,
    val distance: Double,
    val azimuth: Double,
    val elevationAngle: Double,
    val distanceIncline: Double
)

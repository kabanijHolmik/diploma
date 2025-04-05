package model

import kotlin.math.*

object EllipsoidCalculator {
    private const val WGS84_MAJOR_AXIS = 6378137.0     // большая полуось в метрах
    private const val WGS84_MINOR_AXIS = 6356752.314245 // малая полуось в метрах


    private fun calculateFirstEccentricity(majorAxis: Double, minorAxis: Double): Double {
        return (majorAxis.pow(2.0) - minorAxis.pow(2.0)) / majorAxis.pow(2.0)
    }

    private fun calculateSecondEccentricity(majorAxis: Double, minorAxis: Double): Double {
        return (majorAxis.pow(2.0) - minorAxis.pow(2.0)) / minorAxis.pow(2.0)
    }


    fun calculateEllipsoidParameters(majorAxis: Double = WGS84_MAJOR_AXIS, minorAxis: Double = WGS84_MINOR_AXIS): Ellipsoid {
        val firstEccentricity = calculateFirstEccentricity(majorAxis, minorAxis)
        val secondEccentricity = calculateSecondEccentricity(majorAxis, minorAxis)

        return Ellipsoid(majorAxis = majorAxis, minorAxis = minorAxis, firstEccentricity = firstEccentricity, secondEccentricity = secondEccentricity)
    }

}
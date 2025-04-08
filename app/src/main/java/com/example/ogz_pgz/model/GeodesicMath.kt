package com.example.ogz_pgz.model

import model.Coordinate
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeodesicMath {
    // Константы для эллипсоида WGS-84
    const val a = 6378137.0        // большая полуось (м)
    const val f = 1.0 / 298.257223563 // сжатие
    const val b = a * (1.0 - f)    // малая полуось
    const val e_squared = 2.0 * f - f * f // квадрат эксцентриситета

    /**
     * Преобразование градусов в радианы
     */
    fun toRadians(degrees: Double): Double = degrees * Math.PI / 180.0

    /**
     * Преобразование радиан в градусы
     */
    fun toDegrees(radians: Double): Double = radians * 180.0 / Math.PI

    /**
     * Нормализация угла в градусах к интервалу [0, 360)
     */
    fun normalizeAngle(degrees: Double): Double {
        var result = degrees % 360.0
        if (result < 0) result += 360.0
        return result
    }

    /**
     * Вычисление угла возвышения между двумя точками
     */
    fun calculateElevationAngle(start: Coordinate, end: Coordinate, distance: Double): Double {
        val heightDifference = end.altitude - start.altitude
        return toDegrees(Math.atan2(heightDifference, distance))
    }

    /**
     * Вычисление наклонного расстояния между точками с учетом высоты
     */
    fun calculateInclinedDistance(horizontalDistance: Double, start: Coordinate, end: Coordinate): Double {
        val heightDifference = end.altitude - start.altitude
        return Math.sqrt(horizontalDistance * horizontalDistance + heightDifference * heightDifference)
    }
}
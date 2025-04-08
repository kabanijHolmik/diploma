package com.example.ogz_pgz.model

import model.Coordinate
import java.util.Locale


object CoordinateConverter {
    // Паттерн для парсинга строки координат в DMS формате
    private val DMS_PATTERN = Regex(
        """(\d+)°(\d+)'(\d+\.\d+)''([NS]) (\d+)°(\d+)'(\d+\.\d+)''([EW])(\d+\.\d+)"""
    )

    // Паттерн для парсинга строки координат в десятичном формате
    // Обновлен для поддержки как точки, так и запятой в качестве разделителя
    private val DECIMAL_PATTERN = Regex(
        """([-+]?\d+[.,]\d+)\s*,\s*([-+]?\d+[.,]\d+)(?:\s*,\s*([-+]?\d+[.,]\d+))?"""
    )

    private val ANGLE_DMS_PATTERN = Regex(
        """(\d+)°(\d+)'(\d+\.\d+)''"""
    )

    // Конвертация строки DMS формата в Coordinate
    fun fromDMS(input: String): Coordinate {
        val newInput = input.replace('_', '0')
        val matchResult = DMS_PATTERN.matchEntire(newInput)
            ?: throw IllegalArgumentException("Неверный формат DMS строки: $input")

        val (latDeg, latMin, latSec, latDir, lonDeg, lonMin, lonSec, lonDir, alt) = matchResult.destructured

        // Вычисление latitude
        val latitude = latDeg.toDouble() + (latMin.toDouble() / 60) + (latSec.toDouble() / 3600)
        val adjustedLatitude = if (latDir == "S") -latitude else latitude

        // Вычисление longitude
        val longitude = lonDeg.toDouble() + (lonMin.toDouble() / 60) + (lonSec.toDouble() / 3600)
        val adjustedLongitude = if (lonDir == "W") -longitude else longitude

        // Преобразование высоты
        val altitude = alt.toDouble()

        return Coordinate(
            latitude = adjustedLatitude,
            longitude = adjustedLongitude,
            altitude = altitude
        )
    }

    // Конвертация строки в десятичном формате в Coordinate
    fun fromDecimal(input: String): Coordinate {
        val matchResult = DECIMAL_PATTERN.matchEntire(input)
            ?: throw IllegalArgumentException("Неверный формат десятичной строки: $input")

        val groups = matchResult.groupValues

        // Преобразуем строки с заменой запятой на точку для корректного парсинга в Double
        val latitude = groups[1].replace(',', '.').toDouble()
        val longitude = groups[2].replace(',', '.').toDouble()

        // Если высота не указана, устанавливаем значение по умолчанию 0.0
        val altitude = if (groups.size > 3 && groups[3].isNotEmpty())
            groups[3].replace(',', '.').toDouble()
        else 0.0

        return Coordinate(
            latitude = latitude,
            longitude = longitude,
            altitude = altitude
        )
    }

    // Конвертация Coordinate в строку в DMS формате
    fun toDMS(coordinate: Coordinate): String {
        // Работа с latitude
        val absLat = Math.abs(coordinate.latitude)
        val latDeg = absLat.toInt()
        val latMin = ((absLat - latDeg) * 60).toInt()
        val latSec = ((absLat - latDeg - latMin / 60.0) * 3600)
        val latDir = if (coordinate.latitude >= 0) "N" else "S"

        // Работа с longitude
        val absLon = Math.abs(coordinate.longitude)
        val lonDeg = absLon.toInt()
        val lonMin = ((absLon - lonDeg) * 60).toInt()
        val lonSec = ((absLon - lonDeg - lonMin / 60.0) * 3600)
        val lonDir = if (coordinate.longitude >= 0) "E" else "W"

        // Форматирование строки с использованием точки в качестве десятичного разделителя
        return String.format(
            Locale.US,
            "%02d°%02d'%06.3f''%s %02d°%02d'%06.3f''%s%06.2f",
            latDeg, latMin, latSec, latDir,
            lonDeg, lonMin, lonSec, lonDir,
            coordinate.altitude
        )
    }

    // Конвертация Coordinate в строку в десятичном формате
    fun toDecimal(coordinate: Coordinate): String {
        // Используем Locale.US для гарантии использования точки в качестве десятичного разделителя
        return String.format(
            Locale.US,
            "%.14f, %.14f, %.2f",
            coordinate.latitude,
            coordinate.longitude,
            coordinate.altitude
        )
    }

    fun doubleToAngleDMS(angle: Double): String {
        val absAngle = Math.abs(angle)
        val degrees = absAngle.toInt()
        val minutes = ((absAngle - degrees) * 60).toInt()
        val seconds = ((absAngle - degrees - minutes / 60.0) * 3600)

        return String.format(Locale.US, "%02d°%02d'%06.3f''", degrees, minutes, seconds)
    }

    // Конвертация строки формата XX°XX'XX.XXX'' в Double
    fun angleDMSToDouble(angleDMS: String): Double {
        val newAngleDMS = angleDMS.replace('_', '0')
        val matchResult = ANGLE_DMS_PATTERN.matchEntire(newAngleDMS)
            ?: throw IllegalArgumentException("Неверный формат угла DMS: $angleDMS")

        val (degrees, minutes, seconds) = matchResult.destructured

        return degrees.toDouble() + (minutes.toDouble() / 60) + (seconds.toDouble() / 3600)
    }
}
package com.example.ogz_pgz.model

import model.Coordinate


object CoordinateConverter {
    fun fromDegreeToRad(degrees: Double): Double = degrees * (Math.PI / 180.0)

    fun fromRadToDegree(rad: Double): Double = rad * 180.0 / Math.PI

    fun parseCoordinate(input: String): Coordinate {
        // Заменяем символы подчеркивания на "0".
        val sanitizedInput = input.replace("_", "0")

        // Если между широтой и долготой отсутствует пробел, можно разрешить его отсутствие.
        // Используем \s* вместо \s+.
        val regex = Regex(
            """^(\d{3})°(\d{2})'(\d{2}\.\d{2})''N\s*(\d{3})°(\d{2})'(\d{2}\.\d{2})''E(\d{4}\.\d)$"""
        )

        val matchResult = regex.matchEntire(sanitizedInput)
            ?: throw IllegalArgumentException("Input does not match the expected format")

        val (degLatStr, minLatStr, secLatStr, degLonStr, minLonStr, secLonStr, altitudeStr) = matchResult.destructured

        val degLat = degLatStr.toDouble()
        val minLat = minLatStr.toDouble()
        val secLat = secLatStr.toDouble()

        val degLon = degLonStr.toDouble()
        val minLon = minLonStr.toDouble()
        val secLon = secLonStr.toDouble()

        val altitude = altitudeStr.toDouble()

        // Перевод в десятичный формат
        val latitude = degLat * 3600.0 + minLat * 60.0 + secLat
        val longitude = degLon * 3600.0 + minLon * 60.0 + secLon

        return Coordinate(latitude = latitude*100, longitude = longitude*100, altitude = altitude)
    }

    fun formatCoordinate(coordinate: Coordinate): String {
        // Преобразование обратно в секунды (деление на 100, т.к. при парсинге мы умножали на 100)
        val totalSecondsLat = coordinate.latitude / 100.0
        val totalSecondsLon = coordinate.longitude / 100.0

        // Расчет градусов, минут и секунд для широты
        val degreesLat = (totalSecondsLat / 3600.0).toInt()
        val minutesLat = ((totalSecondsLat % 3600.0) / 60.0).toInt()
        val secondsLat = totalSecondsLat % 60.0

        // Расчет градусов, минут и секунд для долготы
        val degreesLon = (totalSecondsLon / 3600.0).toInt()
        val minutesLon = ((totalSecondsLon % 3600.0) / 60.0).toInt()
        val secondsLon = totalSecondsLon % 60.0

        // Форматирование строки с правильным количеством нулей
        val formattedDegLat = String.format("%03d", degreesLat)
        val formattedMinLat = String.format("%02d", minutesLat)
        val formattedSecLat = String.format("%05.2f", secondsLat) // 2 знака после запятой

        val formattedDegLon = String.format("%03d", degreesLon)
        val formattedMinLon = String.format("%02d", minutesLon)
        val formattedSecLon = String.format("%05.2f", secondsLon) // 2 знака после запятой

        val formattedAltitude = String.format("%.1f", coordinate.altitude)

        // Создание конечной строки в формате DDD°MM'SS.SS''N DDD°MM'SS.SS''EDDDD.D
        return "${formattedDegLat}°${formattedMinLat}'${formattedSecLat}''N " +
                "${formattedDegLon}°${formattedMinLon}'${formattedSecLon}''E" +
                formattedAltitude
    }

    fun parseLatLon(input: String): Coordinate {
        try {
            // Предварительная обработка входных данных
            val cleanInput = input.replace("_", "0").replace(",", ".")
            val parts = cleanInput.split(" ")
            if (parts.size != 2) throw IllegalArgumentException("Неверный формат строки координат")

            // Остальной код остается тем же
            val latString = parts[0]
            val lonAltString = parts[1]

            // Находим индекс 'E' - символ разделяющий долготу и высоту
            val eIndex = lonAltString.indexOf('E')
            if (eIndex == -1) throw IllegalArgumentException("Не найден разделитель 'E' между долготой и высотой")

            val lonString = lonAltString.substring(0, eIndex + 1)
            val altString = lonAltString.substring(eIndex + 1)

            // Парсинг широты
            val latDeg = latString.substring(0, 3).toInt()
            val latMin = latString.substring(4, 6).toInt()
            val latSec = latString.substring(7, 12).toFloat()
            val latDirection = latString.last()

            // Парсинг долготы
            val lonDeg = lonString.substring(0, 3).toInt()
            val lonMin = lonString.substring(4, 6).toInt()
            val lonSec = lonString.substring(7, 12).toFloat()
            val lonDirection = lonString.last()

            // Расчет десятичных координат
            var latitude = latDeg + (latMin / 60.0) + (latSec / 3600.0)
            if (latDirection == 'S') latitude = -latitude

            var longitude = lonDeg + (lonMin / 60.0) + (lonSec / 3600.0)
            if (lonDirection == 'W') longitude = -longitude

            // Парсинг высоты
            val altitude = altString.toDouble()

            return Coordinate(latitude = latitude, longitude = longitude, altitude = altitude)
        } catch (e: Exception) {
            throw IllegalArgumentException("Ошибка при разборе строки координат: ${e.message}")
        }
    }

    fun coordinateToString(coordinate: Coordinate): String {
        // Обработка широты
        val lat = coordinate.latitude
        val latDeg = lat.toInt()
        val latMinFull = (lat - latDeg) * 60
        val latMin = latMinFull.toInt()
        val latSec = (latMinFull - latMin) * 60

        // Обработка долготы
        val lon = coordinate.longitude
        val lonDeg = lon.toInt()
        val lonMinFull = (lon - lonDeg) * 60
        val lonMin = lonMinFull.toInt()
        val lonSec = (lonMinFull - lonMin) * 60

        // Форматирование:
        // - Степени: 3 цифры с лидирующими нулями (%03d)
        // - Минуты: 2 цифры с лидирующими нулями (%02d)
        // - Секунды: 2 целых цифры и 2 цифры после запятой, общий формат "%05.2f"
        // - Высота: 3 цифры для целой части и 1 знак после запятой, формат "%05.1f" (например, "000.0")
        val latStr = String.format("%03d°%02d'%05.2f''N", latDeg, latMin, latSec)
        val lonStr = String.format("%03d°%02d'%05.2f''E", lonDeg, lonMin, lonSec)
        val altStr = String.format("%06.1f", coordinate.altitude)

        // Результирующая строка с разделяющим пробелом между широтой и долготой и сразу после долготы идёт значение высоты.
        return "$latStr $lonStr$altStr"
    }

    fun parseAngle(input: String): Double {
        val sanitizedInput = input.replace("_", "0")
        // Регулярное выражение для формата азимута:
        // - degrees: одна или более цифр
        // - минуты: одна или более цифр
        // - секунды: цифры с плавающей точкой (например, "19.83")
        val regex = Regex("""^(\d{1,3})°(\d{1,2})'(\d{1,2}\.\d+)''$""")
        val matchResult = regex.matchEntire(sanitizedInput)
            ?: throw IllegalArgumentException("Input does not match the azimuth format")

        val (degStr, minStr, secStr) = matchResult.destructured

        val degrees = degStr.toDouble()
        val minutes = minStr.toDouble()
        val seconds = secStr.toDouble()

        // Перевод в десятичный формат:
        // d.ddd = degrees + minutes/60 + seconds/3600
        return degrees + minutes / 60.0 + seconds / 3600.0
    }

    /**
     * Преобразует десятичный градус азимута в строку формата "ddd°mm'ss.ss''".
     */
    fun angleToDms(azimuth: Double): String {

        // Определяем целую часть (градусы)
        val degrees = azimuth.toInt()
        // Находим оставшуюся дробную часть и преобразуем её в минуты
        val minutesFull = (azimuth - degrees) * 60.0
        val minutes = minutesFull.toInt()
        // Оставшуюся дробь переводим в секунды
        val seconds = (minutesFull - minutes) * 60.0

        // Форматируем так, чтобы:
        // - градусы — 3 знака с ведущими нулями,
        // - минуты — 2 знака,
        // - секунды — 2 целых цифры и 2 цифры после запятой (например, "19.83").
        return String.format("%03d°%02d'%05.2f''", degrees, minutes, seconds)
    }

}

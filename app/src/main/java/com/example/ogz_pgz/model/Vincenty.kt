package com.example.ogz_pgz.model

import com.example.ogz_pgz.model.GeodesicMath.a
import com.example.ogz_pgz.model.GeodesicMath.b
import com.example.ogz_pgz.model.GeodesicMath.e_squared
import com.example.ogz_pgz.model.GeodesicMath.f
import com.example.ogz_pgz.model.GeodesicMath.normalizeAngle
import com.example.ogz_pgz.model.GeodesicMath.toDegrees
import com.example.ogz_pgz.model.GeodesicMath.toRadians

object Vincenty {
    /**
     * Решение прямой геодезической задачи по формулам Винсента
     *
     * @param lat1 широта начальной точки в градусах
     * @param lon1 долгота начальной точки в градусах
     * @param azimuth азимут в градусах
     * @param distance расстояние в метрах
     * @return пара (широта, долгота) конечной точки в градусах
     */
    fun directProblem(lat1: Double, lon1: Double, azimuth: Double, distance: Double): Pair<Double, Double> {
        // Преобразование в радианы
        val lat1Rad = toRadians(lat1)
        val lon1Rad = toRadians(lon1)
        val alpha1 = toRadians(azimuth)
        val s = distance

        val tanU1 = (1.0 - f) * Math.tan(lat1Rad)
        val U1 = Math.atan(tanU1)
        val sigma1 = Math.atan2(tanU1, Math.cos(alpha1))
        val sinAlpha = Math.cos(U1) * Math.sin(alpha1)
        val cosSqAlpha = 1.0 - sinAlpha * sinAlpha
        val uSq = cosSqAlpha * e_squared / (1.0 - e_squared)

        val A = 1.0 + uSq / 16384.0 * (4096.0 + uSq * (-768.0 + uSq * (320.0 - 175.0 * uSq)))
        val B = uSq / 1024.0 * (256.0 + uSq * (-128.0 + uSq * (74.0 - 47.0 * uSq)))

        var sigma = s / (b * A)
        var sigmaPrev: Double

        var cos2SigmaM: Double
        var sinSigma: Double
        var cosSigma: Double
        var deltaSigma: Double

        // Итерационный процесс
        do {
            sigmaPrev = sigma
            cos2SigmaM = Math.cos(2.0 * sigma1 + sigma)
            sinSigma = Math.sin(sigma)
            cosSigma = Math.cos(sigma)
            deltaSigma = B * sinSigma * (cos2SigmaM + B / 4.0 * (cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM) -
                    B / 6.0 * cos2SigmaM * (-3.0 + 4.0 * sinSigma * sinSigma) * (-3.0 + 4.0 * cos2SigmaM * cos2SigmaM)))
            sigma = s / (b * A) + deltaSigma
        } while (Math.abs(sigma - sigmaPrev) > 1e-12)

        // Вычисление конечной точки
        val sinU1 = Math.sin(U1)
        val cosU1 = Math.cos(U1)

        val lat2Rad = Math.atan2(
            sinU1 * cosSigma + cosU1 * sinSigma * Math.cos(alpha1),
            (1.0 - f) * Math.sqrt(sinAlpha * sinAlpha +
                    (sinU1 * sinSigma - cosU1 * cosSigma * Math.cos(alpha1)) *
                    (sinU1 * sinSigma - cosU1 * cosSigma * Math.cos(alpha1)))
        )

        val lambdaRad = Math.atan2(
            sinSigma * Math.sin(alpha1),
            cosU1 * cosSigma - sinU1 * sinSigma * Math.cos(alpha1)
        )

        val C = f / 16.0 * cosSqAlpha * (4.0 + f * (4.0 - 3.0 * cosSqAlpha))
        val L = lambdaRad - (1.0 - C) * f * sinAlpha * (sigma + C * sinSigma * (cos2SigmaM +
                C * cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM)))

        val lon2Rad = lon1Rad + L

        return Pair(toDegrees(lat2Rad), toDegrees(lon2Rad))
    }

    /**
     * Решение обратной геодезической задачи по формулам Винсента
     *
     * @param lat1 широта начальной точки в градусах
     * @param lon1 долгота начальной точки в градусах
     * @param lat2 широта конечной точки в градусах
     * @param lon2 долгота конечной точки в градусах
     * @return пара (расстояние в метрах, азимут в градусах)
     */
    fun inverseProblem(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Pair<Double, Double> {
        // Преобразование в радианы
        val lat1Rad = toRadians(lat1)
        val lon1Rad = toRadians(lon1)
        val lat2Rad = toRadians(lat2)
        val lon2Rad = toRadians(lon2)

        val L = lon2Rad - lon1Rad

        val tanU1 = (1.0 - f) * Math.tan(lat1Rad)
        val tanU2 = (1.0 - f) * Math.tan(lat2Rad)

        val U1 = Math.atan(tanU1)
        val U2 = Math.atan(tanU2)

        val sinU1 = Math.sin(U1)
        val cosU1 = Math.cos(U1)
        val sinU2 = Math.sin(U2)
        val cosU2 = Math.cos(U2)

        // Итерационный процесс
        var lambda = L
        var lambdaPrev: Double
        val iterLimit = 100
        var iterCount = 0

        var cos2SigmaM: Double
        var sinSigma: Double
        var cosSigma: Double
        var sigma: Double
        var sinLambda: Double
        var cosLambda: Double
        var sinAlpha: Double
        var cosSqAlpha: Double

        do {
            iterCount++
            lambdaPrev = lambda
            sinLambda = Math.sin(lambda)
            cosLambda = Math.cos(lambda)

            sinSigma = Math.sqrt(
                (cosU2 * sinLambda) * (cosU2 * sinLambda) +
                        (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda) * (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda)
            )

            if (sinSigma == 0.0) {
                return Pair(0.0, 0.0) // совпадающие точки
            }

            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda
            sigma = Math.atan2(sinSigma, cosSigma)

            sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma
            cosSqAlpha = 1.0 - sinAlpha * sinAlpha

            cos2SigmaM = if (cosSqAlpha != 0.0) {
                cosSigma - 2.0 * sinU1 * sinU2 / cosSqAlpha
            } else {
                0.0 // экваториальная линия
            }

            val C = f / 16.0 * cosSqAlpha * (4.0 + f * (4.0 - 3.0 * cosSqAlpha))

            lambda = L + (1.0 - C) * f * sinAlpha * (sigma + C * sinSigma *
                    (cos2SigmaM + C * cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM)))

        } while (Math.abs(lambda - lambdaPrev) > 1e-12 && iterCount < iterLimit)

        if (iterCount >= iterLimit) {
            return Pair(-1.0, -1.0) // формулы не сходятся
        }

        val uSq = cosSqAlpha * (a * a - b * b) / (b * b)
        val A = 1.0 + uSq / 16384.0 * (4096.0 + uSq * (-768.0 + uSq * (320.0 - 175.0 * uSq)))
        val B = uSq / 1024.0 * (256.0 + uSq * (-128.0 + uSq * (74.0 - 47.0 * uSq)))

        val deltaSigma = B * sinSigma * (cos2SigmaM + B / 4.0 * (cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM) -
                B / 6.0 * cos2SigmaM * (-3.0 + 4.0 * sinSigma * sinSigma) * (-3.0 + 4.0 * cos2SigmaM * cos2SigmaM)))

        val s = b * A * (sigma - deltaSigma)

        // Расчет азимута
        val alpha1 = Math.atan2(
            cosU2 * sinLambda,
            cosU1 * sinU2 - sinU1 * cosU2 * cosLambda
        )

        return Pair(s, normalizeAngle(toDegrees(alpha1)))
    }
}

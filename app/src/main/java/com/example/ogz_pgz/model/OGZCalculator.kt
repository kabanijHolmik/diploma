package com.example.ogz_pgz.model

import com.example.ogz_pgz.model.CoordinateConverter.fromDegreeToRad
import com.example.ogz_pgz.model.CoordinateConverter.fromRadToDegree
import com.example.ogz_pgz.model.GeodesicMath.calculateAzimuth
import model.Coordinate
import model.EllipsoidCalculator.calculateEllipsoidParameters
import com.example.ogz_pgz.model.GeodesicMath.computeAdjustedCV
import com.example.ogz_pgz.model.GeodesicMath.computeAdjustedMinorAxis
import com.example.ogz_pgz.model.GeodesicMath.computeAdjustedSV
import com.example.ogz_pgz.model.GeodesicMath.computeAuxiliaryW
import com.example.ogz_pgz.model.GeodesicMath.computeBComponent
import com.example.ogz_pgz.model.GeodesicMath.computeCComponent
import com.example.ogz_pgz.model.GeodesicMath.computeK2Factor
import com.example.ogz_pgz.model.GeodesicMath.computeSigmaOGZ
import model.GeodesicParameters
import kotlin.math.*

object OGZCalculator {

    private fun ifZero(): Double {
        return 10.0.pow(-10)
    }

    private const val A = 6378137.0 // большая полуось
    private const val F = 1.0 / 298.257223563 // сжатие
    private const val B = A * (1.0 - F) // малая полуось

    fun calculate(coordinate1: Coordinate, coordinate2: Coordinate): GeodesicParameters {
        // Преобразуем координаты из формата СК-42 в радианы
        val lat1 = Math.toRadians(coordinate1.latitude / 360000.0)
        val lon1 = Math.toRadians(coordinate1.longitude / 360000.0)
        val lat2 = Math.toRadians(coordinate2.latitude / 360000.0)
        val lon2 = Math.toRadians(coordinate2.longitude / 360000.0)

        // Разница высот
        val heightDiff = coordinate2.altitude - coordinate1.altitude

        // Вычисляем расстояние и азимут по формуле Винсенти
        val (s, alpha1) = inverseVincenty(lat1, lon1, lat2, lon2)

        // Угол возвышения
        val elevationAngle = Math.toDegrees(Math.atan2(heightDiff, s))

        // Наклонное расстояние
        val slantDistance = Math.sqrt(s * s + heightDiff * heightDiff)

        return GeodesicParameters(
            distance = s,
            azimuth = Math.toDegrees(alpha1),
            elevationAngle = elevationAngle,
            distanceIncline = slantDistance
        )
    }

    // Решение обратной геодезической задачи по алгоритму Винсенти
    private fun inverseVincenty(phi1: Double, L1: Double, phi2: Double, L2: Double): Pair<Double, Double> {
        val U1 = Math.atan((1.0 - F) * Math.tan(phi1))
        val U2 = Math.atan((1.0 - F) * Math.tan(phi2))
        val L = L2 - L1

        val sinU1 = Math.sin(U1)
        val cosU1 = Math.cos(U1)
        val sinU2 = Math.sin(U2)
        val cosU2 = Math.cos(U2)

        var lambda = L
        var lambdaP: Double
        var iterations = 0
        var sinSigma: Double
        var cosSigma: Double
        var sigma: Double
        var sinAlpha: Double
        var cosSqAlpha: Double
        var cos2SigmaM: Double

        do {
            val sinLambda = Math.sin(lambda)
            val cosLambda = Math.cos(lambda)

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

            val C = F / 16.0 * cosSqAlpha * (4.0 + F * (4.0 - 3.0 * cosSqAlpha))

            lambdaP = lambda
            lambda = L + (1.0 - C) * F * sinAlpha * (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM)))

            iterations++
        } while (Math.abs(lambda - lambdaP) > 1e-12 && iterations < 100)

        if (iterations >= 100) {
            return Pair(0.0, 0.0) // не сошлось
        }

        val uSq = cosSqAlpha * (A * A - B * B) / (B * B)
        val A_vincenty = 1.0 + uSq / 16384.0 * (4096.0 + uSq * (-768.0 + uSq * (320.0 - 175.0 * uSq)))
        val B_vincenty = uSq / 1024.0 * (256.0 + uSq * (-128.0 + uSq * (74.0 - 47.0 * uSq)))

        val deltaSigma = B_vincenty * sinSigma * (cos2SigmaM + B_vincenty / 4.0 * (cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM) -
                B_vincenty / 6.0 * cos2SigmaM * (-3.0 + 4.0 * sinSigma * sinSigma) * (-3.0 + 4.0 * cos2SigmaM * cos2SigmaM)))

        val s = B * A_vincenty * (sigma - deltaSigma)

        // Азимуты
        val alpha1 = Math.atan2(
            cosU2 * Math.sin(lambda),
            cosU1 * sinU2 - sinU1 * cosU2 * Math.cos(lambda)
        )

        if (alpha1 < 0) {
            return Pair(s, alpha1 + 2 * Math.PI)
        }

        return Pair(s, alpha1)
    }

//    fun calculate(coordinate1: Coordinate, coordinate2: Coordinate): GeodesicParameters {
//        var latitude1 = coordinate1.latitude
//        var longitude1 = coordinate1.longitude
//        var altitude1 = coordinate1.altitude
//
//        var latitude2 = coordinate2.latitude
//        var longitude2 = coordinate2.longitude
//        var altitude2 = coordinate2.altitude
//
//
//        if (abs(latitude1 - latitude2) < 0.0001 && abs(longitude1 - longitude2) < 0.0001) {
//            return GeodesicParameters(
//                distance = 0.0,
//                azimuth = Double.MIN_VALUE,
//                elevationAngle = Double.MIN_VALUE,
//                distanceIncline = 0.0
//            )
//        }
//
//        // Конвертация координат в градусы
//        val latitude1InDegrees = latitude1 / 360000.0
//        val latitude2InDegrees = latitude2 / 360000.0
//        val longitude1InDegrees = longitude1 / 360000.0
//        val longitude2InDegrees = longitude2 / 360000.0
//
//        // Расчёт горизонтального расстояния и азимута
//        val (distance, azimuth) = calculateOGZInDegrees(
//            latitude1InDegrees,
//            longitude1InDegrees,
//            latitude2InDegrees,
//            longitude2InDegrees
//        )
//
//        // Пересчёт высот (предполагается, что высота делится на 100, как в C++ коде)
//        altitude1 = altitude1 / 100.0
//        altitude2 = altitude2 / 100.0
//        val deltaAltitude = altitude2 - altitude1
//
//        // Угол места (UM) вычисляется по арктангенсу перепада высот относительно горизонтального расстояния.
//        var UM = atan(deltaAltitude / distance)
//        UM = UM * 180 / Math.PI
//        // Наклонное расстояние (SI_N) – гипотенуза в прямоугольном треугольнике.
//        val SI_N = sqrt(deltaAltitude * deltaAltitude + distance * distance)
//
//        return GeodesicParameters(
//            distance = distance,
//            azimuth = azimuth,
//            elevationAngle = UM,
//            distanceIncline = SI_N
//        )
//    }

    private fun calculateOGZInDegrees(
        latitude1InDegrees: Double,
        longitude1InDegrees: Double,
        latitude2InDegrees: Double,
        longitude2InDegrees: Double
    ): Pair<Double, Double> {
        // Получаем параметры эллипсоида
        val ellipsoid = calculateEllipsoidParameters()
        val firstEccentricity = ellipsoid.firstEccentricity
        val secondEccentricity = ellipsoid.secondEccentricity
        val minorAxis = ellipsoid.minorAxis
        val majorAxis = ellipsoid.majorAxis

        // Преобразование градусов в радианы
        val latitude1InRad = fromDegreeToRad(latitude1InDegrees)
        val longitude1InRad = fromDegreeToRad(longitude1InDegrees)
        val latitude2InRad = fromDegreeToRad(latitude2InDegrees)
        val longitude2InRad = fromDegreeToRad(longitude2InDegrees)

        val w1 = computeAuxiliaryW(firstEccentricity, latitude1InRad)
        val w2 = computeAuxiliaryW(firstEccentricity, latitude2InRad)

        val sv1 = computeAdjustedSV(latitude1InRad, w1, firstEccentricity)
        val sv2 = computeAdjustedSV(latitude2InRad, w2, firstEccentricity)

        val cv1 = computeAdjustedCV(latitude1InRad, w1)
        val cv2 = computeAdjustedCV(latitude2InRad, w2)

        val sinProduct = sv1 * sv2            // Произведение синусов широт двух точек
        val cosProduct = cv1 * cv2            // Произведение косинусов широт двух точек
        val crossTerm1 = cv1 * sv2            // Перекрёстный термин 1: cos(latitude1) * sin(latitude2)
        val crossTerm2 = sv1 * cv2            // Перекрёстный термин 2: sin(latitude1) * cos(latitude2)

        val deltaLongitude = longitude2InRad - longitude1InRad

        // Получаем промежуточные значения через gotoPoint62
        val adjustedParams = adjustGeodeticParameters(
            deltaLongitude = deltaLongitude,
            cv1 = cv1,
            cv2 = cv2,
            cosProduct = cosProduct,
            sinProduct = sinProduct,
            crossTerm1 = crossTerm1,
            crossTerm2 = crossTerm2,
            firstEccentricity = firstEccentricity
        )
//ca02, x, sigma, A1, currentAdjustment
        val adjustedCoefficient = adjustedParams[0]
        val intermediateX = adjustedParams[1]
        val sigmaAngle = adjustedParams[2]
        val initialAzimuthRad = adjustedParams[3]
// Переменная d (adjustedParams[4]) возвращается, но далее не используется

// Вычисление масштабного коэффициента по второму эксцентриситету
        val scaleFactorK2 = computeK2Factor(secondEccentricity, adjustedCoefficient)

// Вычисление коэффициентов A, B и C
        val coefficientA = computeAdjustedMinorAxis(scaleFactorK2, minorAxis)
        val coefficientB = computeBComponent(scaleFactorK2, minorAxis)
        val coefficientC = computeCComponent(scaleFactorK2, minorAxis)

// Расчёт параметров для аппроксимации
        val adjustedB1 = 2.0 * coefficientB / adjustedCoefficient
        val adjustedC1 = 2.0 * coefficientC / adjustedCoefficient.pow(2.0)
        val intermediateY = (adjustedCoefficient.pow(2.0) - 2.0 * intermediateX.pow(2.0)) * cos(sigmaAngle)

// Вычисление геодезической дистанции
        val geodeticDistance = coefficientA * sigmaAngle +
                (adjustedB1 * intermediateX + adjustedC1 * intermediateY) * sin(sigmaAngle)

// Перевод начального азимута из радиан в градусы
        val initialAzimuthDeg = fromRadToDegree(initialAzimuthRad)
        return Pair(geodeticDistance, initialAzimuthDeg)
    }

    private fun adjustGeodeticParameters(
        deltaLongitude: Double,
        initialAdjustment: Double = 0.0,
        cv1: Double,
        cv2: Double,
        crossTerm1: Double,
        crossTerm2: Double,
        sinProduct: Double,
        cosProduct: Double,
        firstEccentricity: Double
    ): DoubleArray {

        // Текущее корректирующее смещение
        var currentAdjustment = initialAdjustment

        // Расчёт скорректированной разницы долгот
        val correctedLongitude = deltaLongitude + currentAdjustment

        // Вычисляем вертикальную компоненту для второй точки
        var verticalComponent = cv2 * kotlin.math.sin(correctedLongitude)
        if (verticalComponent == 0.0) {
            verticalComponent = ifZero()
        }

        // Вычисляем горизонтальную компоненту на основе кросс-коэффициентов
        val horizontalComponent = crossTerm1 - crossTerm2 * kotlin.math.cos(correctedLongitude)

        // Начальный азимут рассчитывается по компонентам
        val initialAzimuth = calculateAzimuth(verticalComponent, horizontalComponent)

        // Компоненты для вычисления угла sigma
        val sigmaNumerator = verticalComponent * kotlin.math.sin(initialAzimuth) +
                horizontalComponent * kotlin.math.cos(initialAzimuth)
        val sigmaDenom = sinProduct + cosProduct * kotlin.math.cos(correctedLongitude)
        val sigma = computeSigmaOGZ(sigmaNumerator, sigmaDenom)

        // Определяем синус компоненты A0
        val sineComponentA0 = cv1 * sin(initialAzimuth)
        // Вычисляем квадрат косинуса A0: 1 - sin²(A0)
        var cosA0Squared = 1.0 - sineComponentA0.pow(2.0)
        if (cosA0Squared == 0.0) {
            cosA0Squared = ifZero()
        }

        // Контрольная величина X
        val controlX = 2 * sinProduct - cosA0Squared * kotlin.math.cos(sigma)

        // Вычисление параметров alpha и beta (разложение по степеням первого эксцентриситета)
        val alpha = (firstEccentricity / 2 +
                firstEccentricity.pow(2.0) / 8 +
                firstEccentricity.pow(3.0) / 16) -
                (firstEccentricity.pow(2.0) / 16 +
                        firstEccentricity.pow(3.0) / 16) * cosA0Squared +
                3 * firstEccentricity.pow(3.0) / 128 * cosA0Squared.pow(2.0)

        val beta = (firstEccentricity.pow(2.0) / 32 +
                firstEccentricity.pow(3.0) / 32) * cosA0Squared -
                firstEccentricity.pow(3.0) / 64 * cosA0Squared.pow(2.0)
        val betaFactor = 2.0 * beta / cosA0Squared

        // Вычисление корректировки delta
        val correctionDelta = (alpha * sigma - betaFactor * controlX * sin(sigma)) * sineComponentA0

        // Определяем разницу между предыдущим и новым корректирующим смещением
        val difference = abs(currentAdjustment - correctionDelta)

        // Если разница превышает пороговое значение, повторяем вычисление с новым смещением
        if (difference > ifZero()) {
            currentAdjustment = correctionDelta
            return adjustGeodeticParameters(
                deltaLongitude,
                currentAdjustment,
                cv1,
                cv2,
                crossTerm1,
                crossTerm2,
                sinProduct,
                cosProduct,
                firstEccentricity
            )
        } else {
            // Возвращаем результирующий массив параметров:
            // [cos²A0, контрольная величина X, sigma, начальный азимут, итоговое смещение]
            return doubleArrayOf(cosA0Squared, controlX, sigma, initialAzimuth, currentAdjustment)
        }
    }
}
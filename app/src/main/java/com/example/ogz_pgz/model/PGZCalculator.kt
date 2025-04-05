package model

import com.example.ogz_pgz.model.CoordinateConverter.fromDegreeToRad
import com.example.ogz_pgz.model.CoordinateConverter.fromRadToDegree
import model.EllipsoidCalculator.calculateEllipsoidParameters
import com.example.ogz_pgz.model.GeodesicMath.calcAlpha
import com.example.ogz_pgz.model.GeodesicMath.calcB2
import com.example.ogz_pgz.model.GeodesicMath.calcBeta
import com.example.ogz_pgz.model.GeodesicMath.calcC2Q1
import com.example.ogz_pgz.model.GeodesicMath.calcC2Q10
import com.example.ogz_pgz.model.GeodesicMath.calcCA0
import com.example.ogz_pgz.model.GeodesicMath.calcCtQ1
import com.example.ogz_pgz.model.GeodesicMath.calcDelta
import com.example.ogz_pgz.model.GeodesicMath.calcGamma
import com.example.ogz_pgz.model.GeodesicMath.calcL2
import com.example.ogz_pgz.model.GeodesicMath.calcS2Q1
import com.example.ogz_pgz.model.GeodesicMath.calcS2Q10
import com.example.ogz_pgz.model.GeodesicMath.calcSV2
import com.example.ogz_pgz.model.GeodesicMath.calcSigma
import com.example.ogz_pgz.model.GeodesicMath.calcSigma0
import com.example.ogz_pgz.model.GeodesicMath.calcp
import com.example.ogz_pgz.model.GeodesicMath.computeAdjustedCV
import com.example.ogz_pgz.model.GeodesicMath.computeAdjustedMinorAxis
import com.example.ogz_pgz.model.GeodesicMath.computeAdjustedSV
import com.example.ogz_pgz.model.GeodesicMath.computeAuxiliaryW
import com.example.ogz_pgz.model.GeodesicMath.computeBComponent
import com.example.ogz_pgz.model.GeodesicMath.computeCComponent
import com.example.ogz_pgz.model.GeodesicMath.computeK2Factor
import kotlin.math.cos
import kotlin.math.sin


object PGZCalculator {
    fun calculate(coordinate: Coordinate, geodesicParameters: GeodesicParameters):Coordinate{
        val latitude = coordinate.latitude/ 360000.0
        val longitude = coordinate.longitude/ 360000.0
        val altitude = coordinate.altitude

        var distance = geodesicParameters.distance
        val azimuth = geodesicParameters.azimuth
        val elevationAngle = geodesicParameters.elevationAngle

        var resAltitude = altitude

        if (elevationAngle != 0.0) {
            val elevationAngleRad = elevationAngle * Math.PI / 180.0
            resAltitude += distance * sin(elevationAngleRad)
            distance *= cos(elevationAngleRad)
        }

        val (resLatitude, resLongitude) = calculatePGZInDegrees(latitude, longitude, distance, azimuth)


        return Coordinate(latitude = resLatitude, longitude = resLongitude, altitude = resAltitude)
    }

    private fun calculatePGZInDegrees(latitude: Double, longitude: Double, distance: Double, azimuth: Double): Pair<Double, Double>{
        val ellipsoid = calculateEllipsoidParameters()

        val firstEccentricity = ellipsoid.firstEccentricity
        val secondEccentricity = ellipsoid.secondEccentricity
        val minorAxis = ellipsoid.minorAxis
        val majorAxis = ellipsoid.majorAxis


        val azimuthRad: Double = fromDegreeToRad(azimuth)
        val latitudeRad: Double = fromDegreeToRad(latitude)
        val longitudeRad: Double = fromDegreeToRad(longitude)

        val w: Double = computeAuxiliaryW(firstEccentricity, latitudeRad)
        val sv: Double = computeAdjustedSV(latitudeRad, w, firstEccentricity)
        val cv: Double = computeAdjustedCV(latitudeRad, w)
        val sineComponentA0: Double = cv * sin(azimuthRad)
        //вт. 18.02 VCoordConverter, OGZCalculator, PGZCalculator *дописать методы -> узнать как происходит преобразование, сделать форматированный ввод и внедрить модель в приложение
        val ctQ1: Double = calcCtQ1(cv, azimuthRad, sv)
        val s2Q1: Double = calcS2Q1(ctQ1)
        val c2Q1: Double = calcC2Q1(ctQ1)
        val ca0: Double = calcCA0(sineComponentA0)
        val k2: Double = computeK2Factor(secondEccentricity, ca0)
        val alpha: Double = calcAlpha(firstEccentricity, ca0)
        val beta: Double = calcBeta(firstEccentricity, ca0)
        val a: Double = computeAdjustedMinorAxis(k2, minorAxis)
        val b: Double = computeBComponent(k2, minorAxis)
        val c: Double = computeCComponent(k2, minorAxis)
        val sigma0: Double = calcSigma0(distance, b, c2Q1, s2Q1, a, c)
        val s: Double = calcS2Q10(s2Q1, sigma0, c2Q1)
        val c2Q10: Double = calcC2Q10(s2Q1, sigma0, c2Q1)
        val sigma: Double = calcSigma(sigma0, b, c, c2Q10, s, a)
        val delta: Double = calcDelta(alpha, beta, s, s2Q1, sineComponentA0, sigma)
        val sv2: Double = calcSV2(sv, sigma, cv, azimuthRad)
        val p: Double = calcp(azimuthRad, sigma, cv, sv)
        val gamma: Double = calcGamma(p, azimuthRad)

        val resLatitude = fromRadToDegree(calcB2(sv2, firstEccentricity))
        val resLongitude = fromRadToDegree(calcL2(longitudeRad, gamma, delta))

        return Pair(resLatitude, resLongitude)
    }
}
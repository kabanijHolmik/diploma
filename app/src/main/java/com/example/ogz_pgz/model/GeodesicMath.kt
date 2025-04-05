package com.example.ogz_pgz.model

import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeodesicMath {
    private fun ifZero(): Double {
        return 10.0.pow(-10)
    }

    fun computeSigmaOGZ(Ssigma: Double, Csigma: Double): Double {
        var sigma = kotlin.math.atan(Ssigma / Csigma)
        if (sigma < 0) sigma *= -1
        if (Csigma < 0) sigma = Math.PI - sigma

        return sigma
    }

    fun computeAuxiliaryW(firstEccentricity: Double, latitudeRadians: Double): Double {
        return sqrt(1.0 - firstEccentricity * kotlin.math.sin(latitudeRadians).pow(2))
    }

    fun computeAdjustedSV(latitudeRadians: Double, w: Double, firstEccentricity: Double): Double {
        val sv = kotlin.math.sin(latitudeRadians) * kotlin.math.sqrt(1.0 - firstEccentricity) / w
        return if (sv == 0.0) {
            ifZero()
        } else {
            sv
        }
    }

    fun computeAdjustedCV(latitudeRadians: Double, w: Double): Double {
        return kotlin.math.cos(latitudeRadians) / w
    }

    fun computeK2Factor(secondEccentricity: Double, adjustedCoefficient: Double): Double {
        return secondEccentricity * adjustedCoefficient
    }



    fun calculateAzimuth(p: Double, q: Double): Double {
        var a1 = kotlin.math.atan2(p, q)

        if (a1 < 0) a1 *= -1
        if (q < 0 && p > 0)
            a1 = Math.PI - a1
        if (q < 0 && p < 0)
            a1 += Math.PI
        if (q > 0 && p < 0)
            a1 = 2 * Math.PI - a1

        return a1
    }

    fun calcAlpha(e12: Double, CA0: Double): Double {
        return (e12 / 2.0 + e12.pow(2.0) / 8.0 + e12.pow(3.0) / 16.0) - (e12.pow(2.0) / 16.0 + e12.pow(3.0) / 16.0) * CA0 +
                3.0 * e12.pow(3.0) / 128.0 * CA0.pow(2.0)
    }

    fun calcBeta(e12: Double, CA0: Double): Double {
        return (e12.pow(2.0) / 32.0 + e12.pow(3.0) / 32.0) * CA0 - e12.pow(3.0) / 64.0 * CA0.pow(2.0)
    }

    fun computeAdjustedMinorAxis(scaleFactorK2: Double, minorAxis: Double): Double {
        return minorAxis * (1.0 +
                scaleFactorK2 / 4.0 -
                3.0 * scaleFactorK2.pow(2.0) / 64.0 +
                5.0 * scaleFactorK2.pow(3.0) / 256.0)
    }

    fun computeBComponent(scaleFactorK2: Double, minorAxis: Double): Double {
        return minorAxis * (scaleFactorK2 / 8.0 -
                scaleFactorK2.pow(2.0) / 32.0 +
                15.0 * scaleFactorK2.pow(3.0) / 1024.0)
    }

    fun computeCComponent(scaleFactorK2: Double, minorAxis: Double): Double {
        return minorAxis * (scaleFactorK2.pow(2.0) / 128.0 -
                3.0 * scaleFactorK2.pow(3.0) / 512.0)
    }

    fun calcCtQ1(CV1: Double, A1: Double, SV1: Double): Double {
        return CV1 * cos(A1) / SV1
    }

    fun calcS2Q1(CtQ1: Double): Double {
        return 2.0 * CtQ1 / (CtQ1.pow(2.0) + 1.0)
    }

    fun calcC2Q1(CtQ1: Double): Double {
        return (CtQ1.pow(2.0) - 1.0) / (CtQ1.pow(2.0) + 1.0)
    }

    fun calcSigma0(S: Double, B: Double, C2Q1: Double, S2Q1: Double, A: Double, C: Double): Double {
        return (S - (B + C * C2Q1) * S2Q1) * (1.0 / A)
    }

    fun calcS2Q10(S2Q1: Double, sigma0: Double, C2Q1: Double): Double {
        return S2Q1 * cos(2.0 * sigma0) + C2Q1 * sin(2.0 * sigma0)
    }

    fun calcC2Q10(S2Q1: Double, sigma0: Double, C2Q1: Double): Double {
        return C2Q1 * cos(2.0 * sigma0) - S2Q1 * sin(2.0 * sigma0)
    }

    fun calcSigma(sigma0: Double, B: Double, C: Double, C2Q10: Double, S2Q10: Double, A: Double): Double {
        return sigma0 + (B + 5 * C * C2Q10) * (S2Q10 / A)
    }

    fun calcDelta(alpha: Double, beta: Double, S2Q10: Double, S2Q1: Double, SA0: Double, sigma: Double): Double {
        return (alpha * sigma + beta * (S2Q10 - S2Q1)) * SA0
    }

    fun calcSV2(SV1: Double, sigma: Double, CV1: Double, A1: Double): Double {
        return SV1 * cos(sigma) + CV1 * cos(A1) * sin(sigma)
    }

    fun calcB2(SV2: Double, e12: Double): Double {
        return atan(SV2 / (sqrt(1.0 - e12) * sqrt(1.0 - SV2.pow(2.0))))
    }

    fun calcp(A1: Double, sigma: Double, CV1: Double, SV1: Double): Double {
        return (sin(A1) * sin(sigma)) / (CV1 * cos(sigma) - SV1 * sin(sigma) * cos(A1))
    }

    fun calcGamma(p: Double, A1: Double): Double {
        var gamma = 0.0
        gamma = atan(p)
        if (gamma < 0) gamma = -1.0 * gamma

        val SinA1: Double = sin(A1)
        if (SinA1 > 0 && p > 0) gamma = gamma
        if (SinA1 > 0 && p < 0) gamma = 180.0 - gamma
        if (SinA1 < 0 && p < 0) gamma = -gamma
        if (SinA1 < 0 && p > 0) gamma = 180.0 - gamma

        return gamma
    }

    fun calcL2(L1: Double, gamma: Double, delta: Double): Double {
        val L2 = L1 + gamma - delta
        //if(L2<0)
        //	L2=L2+360.0;
        return L2
    }

    fun calcCA0(SA0: Double): Double {
        return 1.0 - SA0.pow(2.0)
    }
}
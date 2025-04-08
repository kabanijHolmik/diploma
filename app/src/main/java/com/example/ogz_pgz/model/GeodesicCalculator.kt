package com.example.ogz_pgz.model

import model.Coordinate
import model.GeodesicParameters

interface GeodesicCalculator {
    fun calculateOGZ(start: Coordinate, end: Coordinate): GeodesicParameters
    fun calculatePGZ(start: Coordinate, params: GeodesicParameters): Coordinate
}
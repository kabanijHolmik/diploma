package com.example.ogz_pgz.repository

import android.util.Log
import com.example.ogz_pgz.dao.GeodesicParametersDao
import com.example.ogz_pgz.database.AppDatabase
import com.example.ogz_pgz.entities.CalculationEntity
import com.example.ogz_pgz.entities.CalculationWithRelations
import com.example.ogz_pgz.entities.CoordinateEntity
import com.example.ogz_pgz.entities.GeodesicParametersEntity
import com.example.ogz_pgz.entities.toDomain
import com.example.ogz_pgz.entities.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.Coordinate
import model.GeodesicParameters

class GeodesicRepository(private val database: AppDatabase) {
    // Доступ к DAO
    private val coordinateDao = database.coordinateDao()
    private val parametersDao = database.parametersDao()
    private val calculationDao = database.calculationDao()

    // Потоки данных для UI
    val allOgzCalculations: Flow<List<CalculationWithRelations>> = calculationDao.getOgzCalculations()
    val allPgzCalculations: Flow<List<CalculationWithRelations>> = calculationDao.getPgzCalculations()
    val allCalculations: Flow<List<CalculationWithRelations>> = database.calculationDao().getAllCalculations()
    // Методы для добавления данных
    suspend fun saveOgzCalculation(coordinate1: Coordinate, coordinate2: Coordinate, parameters: GeodesicParameters) {
        // Сохраняем или получаем координаты
        val coord1Id = getOrInsertCoordinate(coordinate1)
        val coord2Id = getOrInsertCoordinate(coordinate2)

        // Сохраняем или получаем параметры
        val paramsId = getOrInsertParameters(parameters)

        // Создаем запись о вычислении
        val calculation = CalculationEntity(
            calculationType = "ОГЗ",
            coordinate1Id = coord1Id,
            coordinate2Id = coord2Id,
            parametersId = paramsId
        )

        calculationDao.insert(calculation)
    }

    suspend fun savePgzCalculation(startCoordinate: Coordinate, parameters: GeodesicParameters, resultCoordinate: Coordinate) {
        // Сохраняем или получаем координаты
        val coord1Id = getOrInsertCoordinate(startCoordinate)
        val coord2Id = getOrInsertCoordinate(resultCoordinate)

        // Сохраняем или получаем параметры
        val paramsId = getOrInsertParameters(parameters)

        // Создаем запись о вычислении
        val calculation = CalculationEntity(
            calculationType = "ПГЗ",
            coordinate1Id = coord1Id,
            coordinate2Id = coord2Id,
            parametersId = paramsId
        )

        calculationDao.insert(calculation)
    }

    private suspend fun getOrInsertCoordinate(coordinate: Coordinate): Long {
        // Проверяем, есть ли уже такая координата
        val existingCoord = coordinateDao.getCoordinate(
            coordinate.latitude,
            coordinate.longitude,
            coordinate.altitude
        )

        return existingCoord?.id ?: coordinateDao.insert(
            CoordinateEntity(
                latitude = coordinate.latitude,
                longitude = coordinate.longitude,
                altitude = coordinate.altitude
            )
        )
    }

    private suspend fun getOrInsertParameters(parameters: GeodesicParameters): Long {
        // Проверяем, есть ли уже такие параметры
        val existingParams = parametersDao.getParameters(
            parameters.distance,
            parameters.azimuth,
            parameters.elevationAngle,
            parameters.distanceIncline
        )

        return existingParams?.id ?: parametersDao.insert(
            GeodesicParametersEntity(
                distance = parameters.distance,
                azimuth = parameters.azimuth,
                elevationAngle = parameters.elevationAngle,
                distanceIncline = parameters.distanceIncline
            )
        )
    }

    fun clearAllData() {
        GlobalScope.launch(Dispatchers.IO) {
            database.clearAllTables()
        }
    }
}
package com.example.ogz_pgz.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.ogz_pgz.entities.CalculationEntity
import com.example.ogz_pgz.entities.CalculationWithRelations
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculationDao {
    @Insert
    suspend fun insert(calculation: CalculationEntity): Long

    @Transaction
    @Query("SELECT * FROM calculations WHERE calculationType = 'ОГЗ' ORDER BY timestamp DESC")
    fun getOgzCalculations(): Flow<List<CalculationWithRelations>>

    @Transaction
    @Query("SELECT * FROM calculations WHERE calculationType = 'ПГЗ' ORDER BY timestamp DESC")
    fun getPgzCalculations(): Flow<List<CalculationWithRelations>>

    @Transaction
    @Query("SELECT * FROM calculations ORDER BY timestamp DESC")
    fun getAllCalculations(): Flow<List<CalculationWithRelations>>
}
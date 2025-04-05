package com.example.ogz_pgz

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ogz_pgz.repository.GeodesicRepository
import kotlinx.coroutines.launch
import model.Coordinate
import model.GeodesicParameters

class PgzViewModel(private val repository: GeodesicRepository): ViewModel() {
    private val _coordinate = MutableLiveData<String>("___°__'__.__''N ___°__'__.__''E____._")
    val coordinate: LiveData<String>
        get() = _coordinate

    private val _resCoordinate = MutableLiveData<String>("Coordinate:")
    val resCoordinate: LiveData<String>
        get() = _resCoordinate

    private val _distance = MutableLiveData<String>()
    val distance: LiveData<String>
        get() = _distance

    private val _azimuth = MutableLiveData<String>("___°__'__.__''")
    val azimuth: LiveData<String>
        get() = _azimuth

    private val _elevation = MutableLiveData<String>("___°__'__.__''")
    val elevation: LiveData<String>
        get() = _elevation

    fun setParams(coordinate: String, distance: String, elevation: String, azimuth: String){
        _coordinate.value = coordinate
        _distance.value = distance
        _elevation.value = elevation
        _azimuth.value = azimuth

    }

    fun setCoordinate(coordinate: String){
        _coordinate.value = coordinate
    }

    fun setResult(resCoordinate: String){
        _resCoordinate.value = resCoordinate
    }

    fun saveCalculation(startCoordinate: Coordinate, parameters: GeodesicParameters, resultCoordinate: Coordinate) {
        viewModelScope.launch {
            repository.savePgzCalculation(startCoordinate, parameters, resultCoordinate)
        }
    }

    // Получение истории вычислений
    val calculationHistory = repository.allPgzCalculations
}
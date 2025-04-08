package com.example.ogz_pgz

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ogz_pgz.repository.GeodesicRepository
import kotlinx.coroutines.launch
import model.Coordinate
import model.GeodesicParameters

class OgzViewModel(private val repository: GeodesicRepository): ViewModel() {

    private val _coordinate1 = MutableLiveData<String>("__°__'__.__''N __°__'__.__''E____.__")
    val coordinate1: LiveData<String>
        get() = _coordinate1

    private val _coordinate2 = MutableLiveData<String>("__°__'__.__''N __°__'__.__''E____.__")
    val coordinate2: LiveData<String>
        get() = _coordinate2

    private val _distanceTopo = MutableLiveData<String>("Distance topo, m")
    val distanceTopo: LiveData<String>
        get() = _distanceTopo

    private val _distanceLong = MutableLiveData<String>("Distance long, m")
    val distanceLong: LiveData<String>
        get() = _distanceLong

    private val _elevationAngle = MutableLiveData<String>("Elevation angle, degree")
    val elevationAngle: LiveData<String>
        get() = _elevationAngle

    private val _azimuth = MutableLiveData<String>("Azimuth, degree")
    val azimuth: LiveData<String>
        get() = _azimuth

    fun setGeoParams(distanceTopo: String, distanceLong: String, elevationAngle: String, azimuth: String){
        _distanceTopo.value = distanceTopo
        _distanceLong.value = distanceLong
        _elevationAngle.value = elevationAngle
        _azimuth.value = azimuth

    }

    fun setFirstCoordinate(coordinate: String){
        _coordinate1.value = coordinate
    }

    fun setCoordinates(coordinate1: String, coordinate2: String){
        _coordinate1.value = coordinate1
        _coordinate2.value = coordinate2
    }

    fun saveCalculation(coordinate1: Coordinate, coordinate2: Coordinate, parameters: GeodesicParameters) {
        viewModelScope.launch {
            repository.saveOgzCalculation(coordinate1, coordinate2, parameters)
        }
    }

    // Получение истории вычислений
    val calculationHistory = repository.allOgzCalculations
}
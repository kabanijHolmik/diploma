package com.example.ogz_pgz

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import model.Coordinate

class MapViewModel: ViewModel() {

    private val _point1 = MutableLiveData<Coordinate?>()
    val point1: LiveData<Coordinate?> = _point1

    private val _point2 = MutableLiveData<Coordinate?>()
    val point2: LiveData<Coordinate?> = _point2

    fun updateMapPoints(newPoint1: Coordinate, newPoint2: Coordinate) {
        _point1.value = newPoint1
        _point2.value = newPoint2
    }

    fun updatePoint1(newPoint: Coordinate) {
        _point1.value = newPoint
    }

    fun updatePoint2(newPoint: Coordinate) {
        _point2.value = newPoint
    }

    fun clearMapPoints() {
        _point1.value = null
        _point2.value = null
    }

}
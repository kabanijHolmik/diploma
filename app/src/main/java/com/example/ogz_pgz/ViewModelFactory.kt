package com.example.ogz_pgz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ogz_pgz.repository.GeodesicRepository

class ViewModelFactory(private val repository: GeodesicRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OgzViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OgzViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(PgzViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PgzViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
package com.example.ogz_pgz

import android.app.Application
import com.example.ogz_pgz.database.AppDatabase
import com.example.ogz_pgz.repository.GeodesicRepository

class MainApplication: Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { GeodesicRepository(database) }
}
package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_cities")
data class RecentCityEntity(
    @PrimaryKey
    val key: String, // e.g. "Paris_FR"
    val cityName: String,
    val country: String,
    val adminArea: String?,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

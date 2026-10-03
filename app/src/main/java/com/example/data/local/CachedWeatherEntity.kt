package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_weather")
data class CachedWeatherEntity(
    @PrimaryKey
    val id: Int = 1,
    val cityName: String,
    val country: String,
    val adminArea: String?,
    val latitude: Double,
    val longitude: Double,
    val isGpsLocation: Boolean,
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double,
    val weatherCode: Int,
    val humidityPercentage: Int,
    val windSpeedKmh: Double,
    val windDirectionDegrees: Int,
    val highTempCelsius: Double,
    val lowTempCelsius: Double,
    val uvIndex: Double,
    val pressureHpa: Double,
    val isDay: Boolean,
    val forecastJson: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

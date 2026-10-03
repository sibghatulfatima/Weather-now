package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * High-level weather condition categorized from WMO weather codes.
 */
enum class WeatherCondition(
    val displayName: String,
    val icon: ImageVector,
    val description: String
) {
    SUNNY("Sunny", Icons.Default.WbSunny, "Clear sky with abundant sunshine"),
    MAINLY_CLEAR("Mainly Clear", Icons.Default.WbSunny, "Mostly sunny with slight clouds"),
    PARTLY_CLOUDY("Partly Cloudy", Icons.Default.Cloud, "Sun shining through scattered clouds"),
    OVERCAST("Overcast", Icons.Default.Cloud, "Sky covered with dense clouds"),
    FOGGY("Foggy", Icons.Default.Grain, "Mist or low visibility fog"),
    DRIZZLE("Drizzle", Icons.Default.Grain, "Light intermittent drizzle"),
    RAINY("Rainy", Icons.Default.WaterDrop, "Moderate to steady rainfall"),
    HEAVY_RAIN("Heavy Rain", Icons.Default.WaterDrop, "Torrential downpours and heavy rain"),
    SNOWY("Snowy", Icons.Default.AcUnit, "Falling snow and icy flurries"),
    THUNDERSTORM("Thunderstorm", Icons.Default.Thunderstorm, "Thunderstorms with lightning strikes"),
    UNKNOWN("Clear", Icons.Default.WbSunny, "Pleasant weather conditions");

    companion object {
        fun fromWmoCode(code: Int?): WeatherCondition {
            return when (code) {
                0 -> SUNNY
                1 -> MAINLY_CLEAR
                2 -> PARTLY_CLOUDY
                3 -> OVERCAST
                45, 48 -> FOGGY
                51, 53, 55, 56, 57 -> DRIZZLE
                61, 63, 80, 81 -> RAINY
                65, 66, 67, 82 -> HEAVY_RAIN
                71, 73, 75, 77, 85, 86 -> SNOWY
                95, 96, 99 -> THUNDERSTORM
                else -> MAINLY_CLEAR
            }
        }
    }
}

/**
 * Domain representation of current weather snapshot.
 */
data class CurrentWeather(
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double,
    val condition: WeatherCondition,
    val humidityPercentage: Int,
    val windSpeedKmh: Double,
    val windDirectionDegrees: Int,
    val highTempCelsius: Double,
    val lowTempCelsius: Double,
    val uvIndex: Double,
    val pressureHpa: Double,
    val isDay: Boolean = true
)

/**
 * Single day forecast snapshot for the 5-day forecast.
 */
data class DailyForecast(
    val date: String,
    val dayName: String,
    val condition: WeatherCondition,
    val minTempCelsius: Double,
    val maxTempCelsius: Double,
    val precipitationProbabilityPercentage: Int
)

/**
 * Geo location details for the selected city.
 */
data class CityLocation(
    val name: String,
    val country: String,
    val adminArea: String? = null,
    val latitude: Double,
    val longitude: Double,
    val isCurrentGpsLocation: Boolean = false
) {
    val fullDisplayName: String
        get() = when {
            adminArea.isNullOrBlank() && country.isBlank() -> name
            adminArea.isNullOrBlank() -> "$name, $country"
            else -> "$name, $adminArea, $country"
        }
}

/**
 * UI State for Weather screen.
 */
sealed interface WeatherUiState {
    data class Loading(
        val previousData: Success? = null
    ) : WeatherUiState

    data class Success(
        val location: CityLocation,
        val current: CurrentWeather,
        val forecast: List<DailyForecast>,
        val isRefreshing: Boolean = false,
        val lastUpdatedTimestamp: Long = System.currentTimeMillis()
    ) : WeatherUiState

    data class Error(
        val message: String,
        val cachedData: Success? = null
    ) : WeatherUiState
}

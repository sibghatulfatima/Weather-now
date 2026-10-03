package com.example.data.repository

import com.example.data.local.CachedWeatherDao
import com.example.data.local.CachedWeatherEntity
import com.example.data.local.RecentCityDao
import com.example.data.local.RecentCityEntity
import com.example.data.network.NetworkClient
import com.example.data.network.WeatherApiService
import com.example.model.CityLocation
import com.example.model.CurrentWeather
import com.example.model.DailyForecast
import com.example.model.WeatherCondition
import com.example.model.WeatherUiState
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class WeatherRepository(
    private val apiService: WeatherApiService = NetworkClient.weatherApiService,
    private val cachedWeatherDao: CachedWeatherDao,
    private val recentCityDao: RecentCityDao
) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val forecastListAdapter = moshi.adapter<List<DailyForecastDto>>(
        Types.newParameterizedType(List::class.java, DailyForecastDto::class.java)
    )

    data class DailyForecastDto(
        val date: String,
        val dayName: String,
        val conditionCode: Int,
        val minTempCelsius: Double,
        val maxTempCelsius: Double,
        val precipitationProbability: Int
    )

    suspend fun fetchWeather(location: CityLocation): Result<Pair<CurrentWeather, List<DailyForecast>>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getForecast(
                    latitude = location.latitude,
                    longitude = location.longitude
                )

                val currentDto = response.current ?: throw IllegalStateException("Current weather data unavailable")
                val dailyDto = response.daily

                val condition = WeatherCondition.fromWmoCode(currentDto.weatherCode)

                // Today's high & low from daily forecast if available, otherwise current temp
                val todayHigh = dailyDto?.temperature2mMax?.firstOrNull() ?: (currentDto.temperature2m ?: 20.0)
                val todayLow = dailyDto?.temperature2mMin?.firstOrNull() ?: (currentDto.temperature2m ?: 15.0)

                val currentWeather = CurrentWeather(
                    temperatureCelsius = currentDto.temperature2m ?: 20.0,
                    feelsLikeCelsius = currentDto.apparentTemperature ?: currentDto.temperature2m ?: 20.0,
                    condition = condition,
                    humidityPercentage = currentDto.relativeHumidity2m ?: 50,
                    windSpeedKmh = currentDto.windSpeed10m ?: 10.0,
                    windDirectionDegrees = currentDto.windDirection10m ?: 0,
                    highTempCelsius = todayHigh,
                    lowTempCelsius = todayLow,
                    uvIndex = dailyDto?.uvIndexMax?.firstOrNull() ?: 3.0,
                    pressureHpa = currentDto.surfacePressure ?: 1013.0,
                    isDay = (currentDto.isDay ?: 1) == 1
                )

                // Build 5-day forecast
                val forecastList = mutableListOf<DailyForecast>()
                val times = dailyDto?.time ?: emptyList()
                val weatherCodes = dailyDto?.weatherCode ?: emptyList()
                val maxTemps = dailyDto?.temperature2mMax ?: emptyList()
                val minTemps = dailyDto?.temperature2mMin ?: emptyList()
                val precipProbs = dailyDto?.precipitationProbabilityMax ?: emptyList()

                val count = minOf(5, times.size)
                for (i in 0 until count) {
                    val dateStr = times.getOrNull(i) ?: ""
                    val code = weatherCodes.getOrNull(i) ?: 0
                    val dayName = formatDayName(dateStr, i)
                    val maxT = maxTemps.getOrNull(i) ?: 20.0
                    val minT = minTemps.getOrNull(i) ?: 14.0
                    val prob = precipProbs.getOrNull(i) ?: 0

                    forecastList.add(
                        DailyForecast(
                            date = dateStr,
                            dayName = dayName,
                            condition = WeatherCondition.fromWmoCode(code),
                            minTempCelsius = minT,
                            maxTempCelsius = maxT,
                            precipitationProbabilityPercentage = prob
                        )
                    )
                }

                // Cache in Room
                val forecastDtos = forecastList.map {
                    DailyForecastDto(
                        date = it.date,
                        dayName = it.dayName,
                        conditionCode = it.condition.ordinal,
                        minTempCelsius = it.minTempCelsius,
                        maxTempCelsius = it.maxTempCelsius,
                        precipitationProbability = it.precipitationProbabilityPercentage
                    )
                }
                val forecastJson = forecastListAdapter.toJson(forecastDtos)

                cachedWeatherDao.saveCachedWeather(
                    CachedWeatherEntity(
                        id = 1,
                        cityName = location.name,
                        country = location.country,
                        adminArea = location.adminArea,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        isGpsLocation = location.isCurrentGpsLocation,
                        temperatureCelsius = currentWeather.temperatureCelsius,
                        feelsLikeCelsius = currentWeather.feelsLikeCelsius,
                        weatherCode = currentDto.weatherCode ?: 0,
                        humidityPercentage = currentWeather.humidityPercentage,
                        windSpeedKmh = currentWeather.windSpeedKmh,
                        windDirectionDegrees = currentWeather.windDirectionDegrees,
                        highTempCelsius = currentWeather.highTempCelsius,
                        lowTempCelsius = currentWeather.lowTempCelsius,
                        uvIndex = currentWeather.uvIndex,
                        pressureHpa = currentWeather.pressureHpa,
                        isDay = currentWeather.isDay,
                        forecastJson = forecastJson,
                        lastUpdated = System.currentTimeMillis()
                    )
                )

                // Save recent search if not GPS placeholder
                if (!location.isCurrentGpsLocation) {
                    recentCityDao.insertCity(
                        RecentCityEntity(
                            key = "${location.name}_${location.country}",
                            cityName = location.name,
                            country = location.country,
                            adminArea = location.adminArea,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                Result.success(Pair(currentWeather, forecastList))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getCachedWeather(): WeatherUiState.Success? = withContext(Dispatchers.IO) {
        val entity = cachedWeatherDao.getCachedWeatherOnce() ?: return@withContext null
        try {
            val forecastDtos = forecastListAdapter.fromJson(entity.forecastJson) ?: emptyList()
            val forecastList = forecastDtos.map {
                val condition = WeatherCondition.values().getOrElse(it.conditionCode) { WeatherCondition.MAINLY_CLEAR }
                DailyForecast(
                    date = it.date,
                    dayName = it.dayName,
                    condition = condition,
                    minTempCelsius = it.minTempCelsius,
                    maxTempCelsius = it.maxTempCelsius,
                    precipitationProbabilityPercentage = it.precipitationProbability
                )
            }

            val condition = WeatherCondition.fromWmoCode(entity.weatherCode)
            val current = CurrentWeather(
                temperatureCelsius = entity.temperatureCelsius,
                feelsLikeCelsius = entity.feelsLikeCelsius,
                condition = condition,
                humidityPercentage = entity.humidityPercentage,
                windSpeedKmh = entity.windSpeedKmh,
                windDirectionDegrees = entity.windDirectionDegrees,
                highTempCelsius = entity.highTempCelsius,
                lowTempCelsius = entity.lowTempCelsius,
                uvIndex = entity.uvIndex,
                pressureHpa = entity.pressureHpa,
                isDay = entity.isDay
            )

            val location = CityLocation(
                name = entity.cityName,
                country = entity.country,
                adminArea = entity.adminArea,
                latitude = entity.latitude,
                longitude = entity.longitude,
                isCurrentGpsLocation = entity.isGpsLocation
            )

            WeatherUiState.Success(
                location = location,
                current = current,
                forecast = forecastList,
                isRefreshing = false,
                lastUpdatedTimestamp = entity.lastUpdated
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchCities(query: String): Result<List<CityLocation>> = withContext(Dispatchers.IO) {
        try {
            val trimmed = query.trim()
            if (trimmed.length < 2) {
                return@withContext Result.success(emptyList())
            }

            val response = apiService.searchCities(name = trimmed)
            val results = response.results?.map {
                CityLocation(
                    name = it.name,
                    country = it.country ?: it.countryCode ?: "",
                    adminArea = it.admin1,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    isCurrentGpsLocation = false
                )
            } ?: emptyList()

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getRecentCities(): Flow<List<CityLocation>> {
        return recentCityDao.getRecentCities().map { entities ->
            entities.map {
                CityLocation(
                    name = it.cityName,
                    country = it.country,
                    adminArea = it.adminArea,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    isCurrentGpsLocation = false
                )
            }
        }
    }

    suspend fun saveRecentCity(location: CityLocation) = withContext(Dispatchers.IO) {
        recentCityDao.insertCity(
            RecentCityEntity(
                key = "${location.name}_${location.country}",
                cityName = location.name,
                country = location.country,
                adminArea = location.adminArea,
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    private fun formatDayName(dateString: String, index: Int): String {
        if (index == 0) return "Today"
        if (index == 1) return "Tomorrow"
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateString) ?: return dateString
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            dayFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }
}

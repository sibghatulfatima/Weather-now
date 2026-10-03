package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedWeatherDao {
    @Query("SELECT * FROM cached_weather WHERE id = 1 LIMIT 1")
    fun getCachedWeather(): Flow<CachedWeatherEntity?>

    @Query("SELECT * FROM cached_weather WHERE id = 1 LIMIT 1")
    suspend fun getCachedWeatherOnce(): CachedWeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCachedWeather(weather: CachedWeatherEntity)

    @Query("DELETE FROM cached_weather")
    suspend fun clearCache()
}

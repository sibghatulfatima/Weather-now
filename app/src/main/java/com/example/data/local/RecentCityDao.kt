package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentCityDao {
    @Query("SELECT * FROM recent_cities ORDER BY timestamp DESC LIMIT 10")
    fun getRecentCities(): Flow<List<RecentCityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: RecentCityEntity)

    @Query("DELETE FROM recent_cities WHERE `key` = :key")
    suspend fun deleteCity(key: String)

    @Query("DELETE FROM recent_cities")
    suspend fun clearAll()
}

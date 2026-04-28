package com.example.theglobalcarbonfootprintproject.data.local.dao

import androidx.room.*
import com.example.theglobalcarbonfootprintproject.data.local.entities.FoodLog
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: FoodLog)

    @Query("SELECT * FROM food_logs WHERE date >= :startOfDay ORDER BY date DESC")
    fun getTodayLogs(startOfDay: Long): Flow<List<FoodLog>>

    @Query("SELECT SUM(co2Kg) FROM food_logs WHERE date >= :startOfDay")
    fun getTodayFoodCo2(startOfDay: Long): Flow<Double?>

    @Query("SELECT * FROM food_logs ORDER BY date DESC")
    fun getAllHistory(): Flow<List<FoodLog>>

    @Query("SELECT EXISTS(SELECT 1 FROM food_logs WHERE date >= :startOfDay)")
    fun hasLoggedToday(startOfDay: Long): Flow<Boolean>
}

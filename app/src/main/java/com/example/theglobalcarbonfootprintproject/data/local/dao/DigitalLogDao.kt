package com.example.theglobalcarbonfootprintproject.data.local.dao

import androidx.room.*
import com.example.theglobalcarbonfootprintproject.data.local.entities.DigitalLog
import kotlinx.coroutines.flow.Flow

@Dao
interface DigitalLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: DigitalLog)

    @Query("SELECT * FROM digital_logs WHERE date >= :startOfDay ORDER BY date DESC")
    fun getTodayLogs(startOfDay: Long): Flow<List<DigitalLog>>

    @Query("SELECT MAX(co2Kg) FROM digital_logs WHERE date >= :startOfDay")
    fun getTodayDigitalCo2(startOfDay: Long): Flow<Double?>

    @Query("SELECT MAX(screenTimeMinutes) FROM digital_logs WHERE date >= :startOfDay")
    fun getTodayDigitalMinutes(startOfDay: Long): Flow<Long?>

    @Query("SELECT MAX(systemTimeMinutes) FROM digital_logs WHERE date >= :startOfDay")
    fun getTodaySystemMinutes(startOfDay: Long): Flow<Long?>



    @Query("SELECT * FROM digital_logs ORDER BY date DESC")
    fun getAllHistory(): Flow<List<DigitalLog>>
}

package com.example.theglobalcarbonfootprintproject.data.local.dao

import androidx.room.*
import com.example.theglobalcarbonfootprintproject.data.local.entities.EnergyLog
import kotlinx.coroutines.flow.Flow

@Dao
interface EnergyLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: EnergyLog)

    @Query("SELECT * FROM energy_logs WHERE date >= :startOfDay ORDER BY date DESC")
    fun getTodayLogs(startOfDay: Long): Flow<List<EnergyLog>>

    @Query("SELECT SUM(co2Kg) FROM energy_logs WHERE date >= :startOfDay")
    fun getTodayEnergyCo2(startOfDay: Long): Flow<Double?>

    @Query("SELECT * FROM energy_logs ORDER BY date DESC")
    fun getAllHistory(): Flow<List<EnergyLog>>

    @Query("SELECT EXISTS(SELECT 1 FROM energy_logs WHERE date >= :startOfDay)")
    fun hasLoggedToday(startOfDay: Long): Flow<Boolean>
}

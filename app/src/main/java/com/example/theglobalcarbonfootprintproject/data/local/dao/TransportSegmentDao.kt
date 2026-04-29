package com.example.theglobalcarbonfootprintproject.data.local.dao

import androidx.room.*
import com.example.theglobalcarbonfootprintproject.data.local.entities.TransportSegment
import kotlinx.coroutines.flow.Flow

@Dao
interface TransportSegmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(segment: TransportSegment)

    @Query("SELECT * FROM transport_segments WHERE date >= :startOfDay ORDER BY date DESC")
    fun getTodaySegments(startOfDay: Long): Flow<List<TransportSegment>>

    @Query("SELECT SUM(co2Kg) FROM transport_segments WHERE date >= :startOfDay")
    fun getTodayTransportCo2(startOfDay: Long): Flow<Double?>

    @Query("UPDATE transport_segments SET transportMode = :mode, userVerified = 1 WHERE id = :segmentId")
    suspend fun updateMode(segmentId: Int, mode: com.example.theglobalcarbonfootprintproject.calculator.TransportMode)

    @Query("SELECT * FROM transport_segments ORDER BY date DESC")
    fun getAllHistory(): Flow<List<TransportSegment>>

    @Query("SELECT SUM(estimatedKm) FROM transport_segments WHERE date >= :startOfDay AND transportMode != 'WALK_BIKE'")
    fun getTodayMotorizedKm(startOfDay: Long): Flow<Double?>

    @Query("SELECT SUM(estimatedKm) FROM transport_segments WHERE date >= :startOfDay AND transportMode = 'WALK_BIKE'")
    fun getTodayWalkingKm(startOfDay: Long): Flow<Double?>
}


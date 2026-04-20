package com.example.theglobalcarbonfootprintproject.data.local.dao

import androidx.room.*
import com.example.theglobalcarbonfootprintproject.data.local.entities.CarbonLog
import com.example.theglobalcarbonfootprintproject.data.local.entities.RewardRecord
import com.example.theglobalcarbonfootprintproject.data.local.entities.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface CarbonDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Query("SELECT * FROM carbon_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<CarbonLog>>

    @Query("SELECT * FROM carbon_logs WHERE date >= :since ORDER BY date DESC")
    fun getLogsSince(since: Long): Flow<List<CarbonLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CarbonLog)

    @Query("SELECT SUM(pointsEarned) FROM reward_records")
    fun getTotalPoints(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReward(reward: RewardRecord)
}

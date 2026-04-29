package com.example.theglobalcarbonfootprintproject.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import com.example.theglobalcarbonfootprintproject.data.local.dao.*
import com.example.theglobalcarbonfootprintproject.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

import com.example.theglobalcarbonfootprintproject.data.remote.MongoDbManager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Singleton
class CarbonRepository @Inject constructor(
    private val carbonDao: CarbonDao,
    private val transportDao: TransportSegmentDao,
    private val foodDao: FoodLogDao,
    private val energyDao: EnergyLogDao,
    private val digitalDao: DigitalLogDao,
    private val mongoDbManager: MongoDbManager
) {
    fun getUserProfile(): Flow<UserProfile?> = carbonDao.getUserProfile()

    suspend fun saveUserProfile(profile: UserProfile) {
        carbonDao.insertUserProfile(profile)
        Log.d("CarbonRepository", "Saved profile locally.")
        // Save to MongoDB asynchronously without blocking local flow
        CoroutineScope(Dispatchers.IO).launch {
            mongoDbManager.saveUserProfile(profile)
        }
    }

    fun getInstitutionProfile(): Flow<InstitutionProfile?> = carbonDao.getInstitutionProfile()

    suspend fun saveInstitutionProfile(profile: InstitutionProfile) {
        carbonDao.insertInstitutionProfile(profile)
        // Save to MongoDB asynchronously without blocking local flow
        CoroutineScope(Dispatchers.IO).launch {
            mongoDbManager.saveInstitutionProfile(profile)
        }
    }

    fun getAllLogs(): Flow<List<CarbonLog>> = carbonDao.getAllLogs()

    fun getLogsSince(since: Long): Flow<List<CarbonLog>> = carbonDao.getLogsSince(since)

    suspend fun saveLog(log: CarbonLog) {
        carbonDao.insertLog(log)
        CoroutineScope(Dispatchers.IO).launch {
            mongoDbManager.saveLog(log)
        }
    }


    fun getTotalPoints(): Flow<Int?> = carbonDao.getTotalPoints()

    suspend fun saveReward(reward: RewardRecord) = carbonDao.insertReward(reward)

    suspend fun hasRewardForReason(reason: String, dayStart: Long): Boolean =
        carbonDao.hasRewardForReason(reason, dayStart) > 0

    suspend fun clearUserProfile() {
        carbonDao.clearUserProfile()
        carbonDao.clearCarbonLogs()
        // Should we clear other tables too? Probably yes for a full sign out.
    }

    // Transport Segment Methods
    suspend fun saveTransportSegment(segment: TransportSegment) = transportDao.insert(segment)

    fun getTodaySegments(startOfDay: Long): Flow<List<TransportSegment>> =
        transportDao.getTodaySegments(startOfDay)

    fun getTodayTransportCo2(startOfDay: Long): Flow<Double?> =
        transportDao.getTodayTransportCo2(startOfDay)

    suspend fun updateTransportMode(segmentId: Int, mode: com.example.theglobalcarbonfootprintproject.calculator.TransportMode) =
        transportDao.updateMode(segmentId, mode)

    fun getTodayMotorizedKm(startOfDay: Long): Flow<Double?> =
        transportDao.getTodayMotorizedKm(startOfDay)

    fun getTodayWalkingKm(startOfDay: Long): Flow<Double?> =
        transportDao.getTodayWalkingKm(startOfDay)


    // Food Log Methods
    suspend fun saveFoodLog(log: FoodLog) = foodDao.insert(log)
    fun getTodayFoodCo2(startOfDay: Long): Flow<Double?> = foodDao.getTodayFoodCo2(startOfDay)
    fun hasLoggedFoodToday(startOfDay: Long): Flow<Boolean> = foodDao.hasLoggedToday(startOfDay)

    // Energy Log Methods
    suspend fun saveEnergyLog(log: EnergyLog) = energyDao.insert(log)
    fun getTodayEnergyCo2(startOfDay: Long): Flow<Double?> = energyDao.getTodayEnergyCo2(startOfDay)
    fun hasLoggedEnergyToday(startOfDay: Long): Flow<Boolean> = energyDao.hasLoggedToday(startOfDay)

    // Digital Log Methods
    suspend fun saveDigitalLog(log: DigitalLog) = digitalDao.insert(log)
    fun getTodayDigitalCo2(startOfDay: Long): Flow<Double?> = digitalDao.getTodayDigitalCo2(startOfDay)

    suspend fun syncDigitalFootprint(context: Context) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        val stats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        if (!stats.isNullOrEmpty()) {
            val totalTimeMs = stats.values.sumOf { it.totalTimeInForeground }
            val minutes = totalTimeMs / (60 * 1000)
            val co2Kg = (minutes / 60.0) * 0.036
            
            saveDigitalLog(
                DigitalLog(
                    date = endTime,
                    screenTimeMinutes = minutes,
                    co2Kg = co2Kg
                )
            )
        }
    }

    // History Methods
    fun getAllTransportHistory(): Flow<List<TransportSegment>> = transportDao.getAllHistory()
    fun getAllFoodHistory(): Flow<List<FoodLog>> = foodDao.getAllHistory()
    fun getAllEnergyHistory(): Flow<List<EnergyLog>> = energyDao.getAllHistory()

    suspend fun syncCurrentUser() {
        // Disabled MongoDB sync
        Log.d("CarbonRepository", "Sync disabled")
    }
}

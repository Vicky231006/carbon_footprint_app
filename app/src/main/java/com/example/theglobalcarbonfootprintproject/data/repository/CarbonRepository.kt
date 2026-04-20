package com.example.theglobalcarbonfootprintproject.data.repository

import com.example.theglobalcarbonfootprintproject.data.local.dao.CarbonDao
import com.example.theglobalcarbonfootprintproject.data.local.dao.EnergyLogDao
import com.example.theglobalcarbonfootprintproject.data.local.dao.FoodLogDao
import com.example.theglobalcarbonfootprintproject.data.local.dao.TransportSegmentDao
import com.example.theglobalcarbonfootprintproject.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarbonRepository @Inject constructor(
    private val carbonDao: CarbonDao,
    private val transportDao: TransportSegmentDao,
    private val foodDao: FoodLogDao,
    private val energyDao: EnergyLogDao
) {
    fun getUserProfile(): Flow<UserProfile?> = carbonDao.getUserProfile()

    suspend fun saveUserProfile(profile: UserProfile) = carbonDao.insertUserProfile(profile)

    fun getAllLogs(): Flow<List<CarbonLog>> = carbonDao.getAllLogs()

    fun getLogsSince(since: Long): Flow<List<CarbonLog>> = carbonDao.getLogsSince(since)

    suspend fun saveLog(log: CarbonLog) = carbonDao.insertLog(log)

    fun getTotalPoints(): Flow<Int?> = carbonDao.getTotalPoints()

    suspend fun saveReward(reward: RewardRecord) = carbonDao.insertReward(reward)

    // Transport Segment Methods
    suspend fun saveTransportSegment(segment: TransportSegment) = transportDao.insert(segment)

    fun getTodaySegments(startOfDay: Long): Flow<List<TransportSegment>> =
        transportDao.getTodaySegments(startOfDay)

    fun getTodayTransportCo2(startOfDay: Long): Flow<Double?> =
        transportDao.getTodayTransportCo2(startOfDay)

    suspend fun updateTransportMode(segmentId: Int, mode: com.example.theglobalcarbonfootprintproject.calculator.TransportMode) =
        transportDao.updateMode(segmentId, mode)

    // Food Log Methods
    suspend fun saveFoodLog(log: FoodLog) = foodDao.insert(log)
    fun getTodayFoodCo2(startOfDay: Long): Flow<Double?> = foodDao.getTodayFoodCo2(startOfDay)

    // Energy Log Methods
    suspend fun saveEnergyLog(log: EnergyLog) = energyDao.insert(log)
    fun getTodayEnergyCo2(startOfDay: Long): Flow<Double?> = energyDao.getTodayEnergyCo2(startOfDay)

    // History Methods
    fun getAllTransportHistory(): Flow<List<TransportSegment>> = transportDao.getAllHistory()
    fun getAllFoodHistory(): Flow<List<FoodLog>> = foodDao.getAllHistory()
    fun getAllEnergyHistory(): Flow<List<EnergyLog>> = energyDao.getAllHistory()
}

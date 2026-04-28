package com.example.theglobalcarbonfootprintproject.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import com.example.theglobalcarbonfootprintproject.data.local.dao.*
import com.example.theglobalcarbonfootprintproject.data.local.entities.*
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.client.model.Filters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarbonRepository @Inject constructor(
    private val carbonDao: CarbonDao,
    private val transportDao: TransportSegmentDao,
    private val foodDao: FoodLogDao,
    private val energyDao: EnergyLogDao,
    private val digitalDao: DigitalLogDao,
    private val mongoDatabase: MongoDatabase
) {
    fun getUserProfile(): Flow<UserProfile?> = carbonDao.getUserProfile()

    suspend fun saveUserProfile(profile: UserProfile) {
        carbonDao.insertUserProfile(profile)
        Log.d("MongoSync", "Saved profile locally, now triggering sync...")
        syncProfileToMongo(profile)
    }

    private suspend fun testConnection() {
        Log.d("MongoSync", "--- STARTING MONGODB CONNECTION TEST ---")
        try {
            val collection = mongoDatabase.getCollection<org.bson.Document>("connection_test")
            val testDoc = org.bson.Document("test", "ping")
                .append("timestamp", System.currentTimeMillis())
                .append("device", android.os.Build.MODEL)
            
            collection.insertOne(testDoc)
            Log.d("MongoSync", "!!! CONNECTION TEST SUCCESSFUL !!! Document written to 'connection_test' collection.")
        } catch (e: Exception) {
            Log.e("MongoSync", "!!! CONNECTION TEST FAILED !!!", e)
        }
        Log.d("MongoSync", "--- END OF MONGODB CONNECTION TEST ---")
    }

    private suspend fun syncProfileToMongo(profile: UserProfile) {
        Log.d("MongoSync", "Attempting to sync profile: ${profile.name}")
        try {
            val collection = mongoDatabase.getCollection<UserProfile>("user_profiles")
            val filter = Filters.eq("name", profile.name)
            val result = collection.replaceOne(filter, profile, ReplaceOptions().upsert(true))
            Log.d("MongoSync", "Successfully synced individual profile for: ${profile.name}. ModifiedCount: ${result.modifiedCount}, UpsertedId: ${result.upsertedId}")
        } catch (e: Exception) {
            Log.e("MongoSync", "Failed to sync individual profile for ${profile.name}", e)
            // We don't want to crash the app if sync fails. 
            // The local data is already saved.
        }
    }

    fun getInstitutionProfile(): Flow<InstitutionProfile?> = carbonDao.getInstitutionProfile()

    suspend fun saveInstitutionProfile(profile: InstitutionProfile) {
        carbonDao.insertInstitutionProfile(profile)
        syncInstitutionToMongo(profile)
    }

    private suspend fun syncInstitutionToMongo(profile: InstitutionProfile) {
        try {
            val collection = mongoDatabase.getCollection<InstitutionProfile>("institution_profiles")
            val filter = Filters.eq("name", profile.name)
            collection.replaceOne(filter, profile, ReplaceOptions().upsert(true))
            Log.d("MongoSync", "Successfully synced institution profile for: ${profile.name}")
        } catch (e: Exception) {
            Log.e("MongoSync", "Failed to sync institution profile", e)
        }
    }

    fun getAllLogs(): Flow<List<CarbonLog>> = carbonDao.getAllLogs()

    fun getLogsSince(since: Long): Flow<List<CarbonLog>> = carbonDao.getLogsSince(since)

    suspend fun saveLog(log: CarbonLog) {
        carbonDao.insertLog(log)
        syncLogToMongo(log)
    }

    private suspend fun syncLogToMongo(log: CarbonLog) {
        try {
            val collection = mongoDatabase.getCollection<CarbonLog>("carbon_logs")
            collection.insertOne(log)
            Log.d("MongoSync", "Successfully synced carbon log for date: ${log.date}")
        } catch (e: Exception) {
            Log.e("MongoSync", "Failed to sync carbon log", e)
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

    fun getTodayKmWalked(startOfDay: Long): Flow<Double?> =
        transportDao.getTodayKmWalked(startOfDay)

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
        testConnection() // Run the test first
        Log.d("MongoSync", "Fetching current profile for sync...")
        val profile = getUserProfile().first()
        Log.d("MongoSync", "Profile found for sync: ${profile?.name ?: "NULL"}")
        profile?.let { syncProfileToMongo(it) }
        
        val instProfile = getInstitutionProfile().first()
        Log.d("MongoSync", "Institution profile found: ${instProfile?.name ?: "NULL"}")
        instProfile?.let { syncInstitutionToMongo(it) }
    }
}

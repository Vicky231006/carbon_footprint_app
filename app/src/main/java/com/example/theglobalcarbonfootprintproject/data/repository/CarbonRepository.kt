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

    suspend fun getLogCountForDay(dayStart: Long): Int = carbonDao.getLogCountForDay(dayStart)

    suspend fun deleteLogsForDay(dayStart: Long) = carbonDao.deleteLogsForDay(dayStart)



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
    fun getTodayDigitalMinutes(startOfDay: Long): Flow<Long?> = digitalDao.getTodayDigitalMinutes(startOfDay)
    fun getTodaySystemMinutes(startOfDay: Long): Flow<Long?> = digitalDao.getTodaySystemMinutes(startOfDay)



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
            val systemExclusions = setOf(
                "com.android.systemui",
                "com.google.android.googlequicksearchbox",
                "com.android.launcher3",
                "com.sec.android.app.launcher",
                "com.google.android.apps.nexuslauncher",
                "com.miui.home",
                "com.huawei.android.launcher",
                "com.vivo.launcher",
                "com.bbk.launcher2",
                "android",
                "com.android.settings",
                "com.google.android.gms",
                "com.google.android.as",
                "com.google.android.inputmethod.latin",
                "com.samsung.android.honeyboard",
                "com.vivo.systemui"
            )

            val packageManager = context.packageManager
            val interactiveTimeMs = stats.entries
                .filter { pkg -> 
                    val name = pkg.key.lowercase()
                    // 1. Check if it's a known system app
                    val isSystem = systemExclusions.any { exclusion ->
                        if (exclusion == "android") name == "android"
                        else name.startsWith(exclusion) || name == exclusion
                    }
                    if (isSystem) return@filter false
                    
                    // 2. The Gold Standard: If it has a launch intent (icon in drawer), it's interactive
                    packageManager.getLaunchIntentForPackage(pkg.key) != null || 
                    pkg.key == context.packageName // Always include ourselves
                }
                .sumOf { it.value.totalTimeInForeground }



            val totalTimeMs = stats.values.sumOf { it.totalTimeInForeground }
            val systemTimeMs = (totalTimeMs - interactiveTimeMs).coerceAtLeast(0)
                
            val interactiveMins = interactiveTimeMs / (60 * 1000)
            val systemMins = systemTimeMs / (60 * 1000)
            
            Log.d("CarbonRepository", "Bifurcation v2: Interactive=$interactiveMins min, System=$systemMins min")
            
            val co2Kg = (totalTimeMs / (3600.0 * 1000.0)) * 0.036
            
            saveDigitalLog(
                DigitalLog(
                    date = endTime,
                    screenTimeMinutes = interactiveMins,
                    systemTimeMinutes = systemMins,
                    co2Kg = co2Kg
                )
            )
        }




    }

    // History Methods
    fun getAllTransportHistory(): Flow<List<TransportSegment>> = transportDao.getAllHistory()
    fun getAllFoodHistory(): Flow<List<FoodLog>> = foodDao.getAllHistory()
    fun getAllEnergyHistory(): Flow<List<EnergyLog>> = energyDao.getAllHistory()
    fun getAllDigitalHistory(): Flow<List<DigitalLog>> = digitalDao.getAllHistory()

    suspend fun saveAiParsedLog(parsed: com.example.theglobalcarbonfootprintproject.calculator.NlpLogger.ParsedLog) {
        val date = System.currentTimeMillis()
        when (parsed.category.uppercase()) {
            "FOOD" -> {
                val dietType = com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.DietType.valueOf(parsed.data["type"]?.toString() ?: "MIXED")
                val meals = (parsed.data["meals"] as? Number)?.toInt() ?: 1
                val co2 = com.example.theglobalcarbonfootprintproject.calculator.CarbonEngine.calculateFood(dietType, meals)
                
                val mealType = when(dietType) {
                    com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.DietType.VEGAN -> com.example.theglobalcarbonfootprintproject.data.local.entities.MealType.VEGAN
                    com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.DietType.VEGETARIAN -> com.example.theglobalcarbonfootprintproject.data.local.entities.MealType.VEGETARIAN
                    com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.DietType.MIXED -> com.example.theglobalcarbonfootprintproject.data.local.entities.MealType.LOW_MEAT
                    com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.DietType.MEAT_HEAVY -> com.example.theglobalcarbonfootprintproject.data.local.entities.MealType.HIGH_MEAT
                }
                
                saveFoodLog(FoodLog(date = date, mealType = mealType, co2Kg = co2, description = "AI Log: ${dietType.name} meal"))
            }
            "TRANSPORT" -> {
                val modeStr = parsed.data["mode"]?.toString() ?: "CAR"
                val mode = com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.TransportMode.valueOf(modeStr)
                val km = (parsed.data["km"] as? Number)?.toDouble() ?: 5.0
                val fuel = com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.FuelType.PETROL
                
                // 1. Calculate CO2 using onboarding.TransportMode
                val co2 = com.example.theglobalcarbonfootprintproject.calculator.CarbonEngine.calculateTransport(km, mode, fuel)
                
                // 2. Store in DB using calculator.TransportMode
                val calcMode = com.example.theglobalcarbonfootprintproject.calculator.TransportMode.valueOf(mode.name)
                saveTransportSegment(TransportSegment(date = date, activityType = 0, durationMinutes = 0.0, estimatedKm = km, transportMode = calcMode, co2Kg = co2, userVerified = true))
            }


            "ENERGY" -> {
                val kwhVal = (parsed.data["kwh"] as? Number)?.toDouble() ?: 0.0
                val profile = getUserProfile().first()
                val co2 = kwhVal * (profile?.gridFactor ?: 0.8)
                saveEnergyLog(EnergyLog(
                    date = date, 
                    energyType = com.example.theglobalcarbonfootprintproject.data.local.entities.EnergyType.ELECTRICITY, 
                    value = kwhVal, 
                    kwh = kwhVal,
                    co2Kg = co2
                ))
            }
            "EVENT" -> {
                val attendees = (parsed.data["attendees"] as? Number)?.toInt() ?: 0
                val co2 = attendees * 1.5 // Rough average for events
                carbonDao.insertLog(
                    CarbonLog(
                        date = date,
                        transportKg = 0.0,
                        electricityKg = 0.0,
                        digitalKg = 0.0,
                        foodKg = 0.0,
                        wasteKg = 0.0,
                        eventKg = co2,
                        totalKg = co2,
                        greenPoints = 10
                    )
                )
            }
        }
    }

    suspend fun insertDemoData() {
        val cal = Calendar.getInstance()
        // Today is Thursday (Apr 30)
        // Monday (Apr 27), Tuesday (Apr 28), Wednesday (Apr 29)
        
        val daysToLog = listOf(
            Calendar.MONDAY to 8.5,
            Calendar.TUESDAY to 12.2,
            Calendar.WEDNESDAY to 9.8
        )

        daysToLog.forEach { (dayOfWeek, total) ->
            val demoCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
                set(Calendar.HOUR_OF_DAY, 12)
            }
            val date = demoCal.timeInMillis
            
            // Delete existing to avoid double demo data if run twice
            val startOfDay = demoCal.apply { 
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) 
            }.timeInMillis
            deleteLogsForDay(startOfDay)

            carbonDao.insertLog(
                CarbonLog(
                    date = date,
                    transportKg = total * 0.4,
                    electricityKg = total * 0.3,
                    digitalKg = total * 0.1,
                    foodKg = total * 0.2,
                    totalKg = total,
                    greenPoints = (total * 5).toInt()
                )
            )
            
            // Also insert a reward record to make points count in getTotalPoints()
            val points = (total * 5).toInt()
            if (carbonDao.hasRewardForReason("demo_points_$dayOfWeek", startOfDay) == 0) {
                carbonDao.insertReward(com.example.theglobalcarbonfootprintproject.data.local.entities.RewardRecord(
                    date = date,
                    pointsEarned = points,
                    reason = "demo_points_$dayOfWeek"
                ))
            }
        }
    }

    suspend fun syncCurrentUser() {

        // Disabled MongoDB sync
        Log.d("CarbonRepository", "Sync disabled")
    }
}

package com.example.theglobalcarbonfootprintproject.data.remote

import android.content.Context
import android.util.Log
import com.example.theglobalcarbonfootprintproject.data.local.entities.InstitutionProfile
import com.example.theglobalcarbonfootprintproject.data.local.entities.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MongoDbManager @Inject constructor(
    private val apiService: MongoApiService,
    @ApplicationContext private val context: Context
) {

    suspend fun saveUserProfile(profile: UserProfile) {
        withContext(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("carbon_prefs", Context.MODE_PRIVATE)
                val userId = prefs.getString("user_id", null)
                if (userId == null) {
                    Log.e("MongoDbManager", "Cannot save profile: No userId found")
                    return@withContext
                }

                val response = apiService.saveUserProfile(userId, profile)
                if (response.success) {
                    Log.d("MongoDbManager", "Successfully saved UserProfile to Express Backend.")
                } else {
                    Log.e("MongoDbManager", "Backend returned error for UserProfile: ${response.error}")
                }
            } catch (e: Exception) {
                Log.e("MongoDbManager", "Error sending UserProfile to Backend API", e)
            }
        }
    }

    suspend fun saveInstitutionProfile(profile: InstitutionProfile) {
        withContext(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("carbon_prefs", Context.MODE_PRIVATE)
                val userId = prefs.getString("user_id", null)
                if (userId == null) {
                    Log.e("MongoDbManager", "Cannot save institution: No userId found")
                    return@withContext
                }

                val response = apiService.saveInstitutionProfile(userId, profile)
                if (response.success) {
                    Log.d("MongoDbManager", "Successfully saved InstitutionProfile to Express Backend.")
                } else {
                    Log.e("MongoDbManager", "Backend returned error for InstitutionProfile: ${response.error}")
                }
            } catch (e: Exception) {
                Log.e("MongoDbManager", "Error sending InstitutionProfile to Backend API", e)
            }
        }
    }
    suspend fun saveLog(log: com.example.theglobalcarbonfootprintproject.data.local.entities.CarbonLog) {
        withContext(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("carbon_prefs", Context.MODE_PRIVATE)
                val userId = prefs.getString("user_id", null)
                if (userId == null) return@withContext

                val userType = prefs.getString("user_type", "INDIVIDUAL") ?: "INDIVIDUAL"
                val score = prefs.getInt("current_carbon_score", 75)
                val deviceId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown"

                val request = DailyLogRequest(
                    deviceId = deviceId,
                    date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(log.date)),
                    userType = userType,
                    transportKg = log.transportKg,
                    energyKg = log.electricityKg,
                    foodKg = log.foodKg,
                    digitalKg = log.digitalKg,
                    wasteKg = log.wasteKg,
                    eventKg = log.eventKg,
                    totalKg = log.totalKg,
                    score = score,
                    kmWalked = 0.0,
                    stepsCount = prefs.getInt("steps_today", 0),
                    energyLogged = true,
                    foodLogged = true,
                    transportAutoDetected = true,
                    electricityMethod = "Manual",
                    seasonLabel = "Current"
                )

                val response = apiService.syncLog(userId, request)

            } catch (e: Exception) {
                Log.e("MongoDbManager", "Error syncing log", e)
            }
        }
    }

    suspend fun getLeaderboard(state: String?, userType: String): List<com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry> {

        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getLeaderboard(state, userType)
                if (response.success && response.leaderboard != null) {
                    response.leaderboard
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("MongoDbManager", "Error fetching leaderboard", e)
                emptyList()
            }
        }
    }
}



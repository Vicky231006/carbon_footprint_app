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
                val response = apiService.saveInstitutionProfile(profile)
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

                val response = apiService.syncLog(userId, log)
                if (response.success) {
                    Log.d("MongoDbManager", "Successfully synced CarbonLog to Backend.")
                }
            } catch (e: Exception) {
                Log.e("MongoDbManager", "Error syncing log", e)
            }
        }
    }
}


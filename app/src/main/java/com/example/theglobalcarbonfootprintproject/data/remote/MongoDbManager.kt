package com.example.theglobalcarbonfootprintproject.data.remote

import android.util.Log
import com.example.theglobalcarbonfootprintproject.data.local.entities.InstitutionProfile
import com.example.theglobalcarbonfootprintproject.data.local.entities.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MongoDbManager @Inject constructor(
    private val apiService: MongoApiService
) {

    suspend fun saveUserProfile(profile: UserProfile) {
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.saveUserProfile(profile)
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
}

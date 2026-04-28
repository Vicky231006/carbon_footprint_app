package com.example.theglobalcarbonfootprintproject.data.remote

import com.example.theglobalcarbonfootprintproject.data.local.entities.InstitutionProfile
import com.example.theglobalcarbonfootprintproject.data.local.entities.UserProfile
import retrofit2.http.Body
import retrofit2.http.POST

data class ApiResponse(
    val success: Boolean,
    val error: String? = null
)

interface MongoApiService {
    @POST("api/users")
    suspend fun saveUserProfile(@Body profile: UserProfile): ApiResponse

    @POST("api/institutions")
    suspend fun saveInstitutionProfile(@Body profile: InstitutionProfile): ApiResponse
}

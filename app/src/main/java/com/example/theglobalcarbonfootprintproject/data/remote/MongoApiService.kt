package com.example.theglobalcarbonfootprintproject.data.remote

import com.example.theglobalcarbonfootprintproject.data.local.entities.InstitutionProfile
import com.example.theglobalcarbonfootprintproject.data.local.entities.UserProfile
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class ApiResponse(
    val success: Boolean,
    val error: String? = null,
    val userId: String? = null,
    val user: UserProfile? = null,
    val institution: InstitutionProfile? = null,
    val leaderboard: List<LeaderboardEntry>? = null
)


data class LeaderboardEntry(
    val name: String,
    val state: String,
    val baseline_co2_daily: Double
)

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val email: String, val password: String, val name: String, val state: String)

interface MongoApiService {
    @POST("api/auth/register")
    suspend fun registerUser(@Body request: RegisterRequest): ApiResponse

    @POST("api/auth/login")
    suspend fun loginUser(@Body request: LoginRequest): ApiResponse

    @POST("api/users/{userId}")
    suspend fun saveUserProfile(@Path("userId") userId: String, @Body profile: UserProfile): ApiResponse

    @POST("api/institutions/{userId}")
    suspend fun saveInstitutionProfile(@Path("userId") userId: String, @Body profile: InstitutionProfile): ApiResponse


    @GET("api/leaderboard")
    suspend fun getLeaderboard(@Query("state") state: String?, @Query("userType") userType: String?): ApiResponse


    @POST("api/logs/{userId}")
    suspend fun syncLog(@Path("userId") userId: String, @Body request: DailyLogRequest): ApiResponse


    @GET("api/logs/{userId}")
    suspend fun getLogs(@Path("userId") userId: String): LogHistoryResponse
    
    @POST("api/ai/chat")
    suspend fun chatWithAi(@Body request: ChatRequest): ChatResponse
}

data class ChatRequest(val message: String, val context: String)
data class ChatResponse(val success: Boolean, val reply: String? = null, val error: String? = null)


data class LogHistoryResponse(
    val success: Boolean,
    val logs: List<com.example.theglobalcarbonfootprintproject.data.local.entities.CarbonLog>? = null,
    val error: String? = null
)


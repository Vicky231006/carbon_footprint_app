package com.example.theglobalcarbonfootprintproject.data.remote

import com.example.theglobalcarbonfootprintproject.data.local.entities.CarbonLog
import com.example.theglobalcarbonfootprintproject.data.local.entities.FoodLog
import com.example.theglobalcarbonfootprintproject.data.local.entities.TransportSegment
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface UserApiService {
    @POST("daily-log")
    suspend fun syncDailyLog(
        @Header("deviceId") deviceId: String,
        @Body request: DailyLogRequest
    ): Response<Unit>

    @POST("food-log")
    suspend fun syncFoodLog(
        @Header("deviceId") deviceId: String,
        @Body log: FoodLog
    ): Response<Unit>

    @POST("transport-segment")
    suspend fun syncTransportSegment(
        @Header("deviceId") deviceId: String,
        @Body segment: TransportSegment
    ): Response<Unit>
}

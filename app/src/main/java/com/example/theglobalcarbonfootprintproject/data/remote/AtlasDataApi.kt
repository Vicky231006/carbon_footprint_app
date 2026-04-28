package com.example.theglobalcarbonfootprintproject.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class AtlasFilterRequest(
    val dataSource: String = "Cluster0",
    val database: String = "global_carbon_footprint_project",
    val collection: String,
    val document: Any? = null,
    val filter: Any? = null,
    val update: Any? = null,
    val upsert: Boolean = true
)

interface AtlasDataApi {
    @POST("action/insertOne")
    suspend fun insertOne(
        @Header("api-key") apiKey: String,
        @Body request: AtlasFilterRequest
    ): Response<Unit>

    @POST("action/updateOne")
    suspend fun updateOne(
        @Header("api-key") apiKey: String,
        @Body request: AtlasFilterRequest
    ): Response<Unit>
}

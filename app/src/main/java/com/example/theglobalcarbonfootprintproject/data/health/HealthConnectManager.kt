package com.example.theglobalcarbonfootprintproject.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    suspend fun hasAllPermissions(): Boolean {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun getStepsToday(): Long? {
        if (!hasAllPermissions()) return null

        try {
            val now = java.time.ZonedDateTime.now()
            val startOfDay = now.toLocalDate().atStartOfDay(now.zone)
            
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(
                        startOfDay.toInstant(),
                        now.toInstant()
                    )
                )
            )
            val steps = response[StepsRecord.COUNT_TOTAL]
            android.util.Log.d("HealthConnect", "Fetched steps from ${startOfDay} to ${now}: $steps")
            return steps
        } catch (e: Exception) {

            android.util.Log.e("HealthConnect", "Error fetching steps", e)
            return null
        }
    }

    
    fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }
}

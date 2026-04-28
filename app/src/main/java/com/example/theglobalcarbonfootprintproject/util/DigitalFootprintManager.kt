package com.example.theglobalcarbonfootprintproject.util

import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.theglobalcarbonfootprintproject.data.local.entities.DigitalLog
import com.example.theglobalcarbonfootprintproject.data.repository.CarbonRepository
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DigitalFootprintManager @Inject constructor(
    private val context: Context,
    private val repository: CarbonRepository
) {
    suspend fun syncDigitalFootprint() {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        // queryAndAggregateUsageStats requires PACKAGE_USAGE_STATS permission
        val stats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        
        if (!stats.isNullOrEmpty()) {
            val totalTimeMs = stats.values.sumOf { it.totalTimeInForeground }
            val minutes = totalTimeMs / (60 * 1000)
            
            // CO2 Calculation: ~0.036kg per hour
            val co2Kg = (minutes / 60.0) * 0.036
            
            repository.saveDigitalLog(
                DigitalLog(
                    date = endTime,
                    screenTimeMinutes = minutes,
                    co2Kg = co2Kg
                )
            )
        } else {
            // If no stats (permission missing?), at least log a minimal baseline if it's middle of the day?
            // Or just leave at 0. But the user wants it to NOT be 0.
            // Let's check if we have any log today, if not, maybe we can't do much without permission.
        }
    }
}

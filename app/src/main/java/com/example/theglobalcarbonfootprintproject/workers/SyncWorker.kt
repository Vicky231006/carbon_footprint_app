package com.example.theglobalcarbonfootprintproject.workers

import android.content.Context
import android.provider.Settings
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.theglobalcarbonfootprintproject.data.local.dao.CarbonDao
import com.example.theglobalcarbonfootprintproject.data.remote.DailyLogRequest
import com.example.theglobalcarbonfootprintproject.data.remote.UserApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val carbonDao: CarbonDao,
    private val apiService: UserApiService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val deviceId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)

        return try {
            val logs = carbonDao.getAllLogs().first()
            val userProfile = carbonDao.getUserProfile().first()
            val isInst = userProfile?.userType == "institution"

            logs.forEach { log ->
                val request = DailyLogRequest(
                    deviceId = deviceId,
                    date = java.time.Instant.ofEpochMilli(log.date).toString(),
                    userType = if (isInst) "INSTITUTION" else "INDIVIDUAL",
                    transportKg = log.transportKg,
                    energyKg = log.electricityKg,
                    foodKg = log.foodKg,
                    digitalKg = log.digitalKg,
                    wasteKg = log.wasteKg,
                    eventKg = log.eventKg,
                    totalKg = log.totalKg,
                    score = 0, // Simplified for worker
                    kmWalked = 0.0,
                    stepsCount = 0,
                    energyLogged = true,
                    foodLogged = true,
                    transportAutoDetected = true,
                    electricityMethod = "historical_sync",
                    seasonLabel = ""
                )
                apiService.syncDailyLog(deviceId, request)
            }
            // Ideally, we'd also sync food logs and transport segments here
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

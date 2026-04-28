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
        return Result.success() // Temporarily disabled remote sync
    }
}

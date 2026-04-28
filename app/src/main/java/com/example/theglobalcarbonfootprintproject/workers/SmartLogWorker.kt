package com.example.theglobalcarbonfootprintproject.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.theglobalcarbonfootprintproject.MainActivity
import com.example.theglobalcarbonfootprintproject.R
import com.example.theglobalcarbonfootprintproject.data.repository.CarbonRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class SmartLogWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: CarbonRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val type = inputData.getString("notification_type") ?: return Result.failure()
        val startOfDay = getStartOfDay()

        // Auto-fetch screen time for Digital Footprint
        if (type == "EVENING" || type == "MID_DAY") {
            fetchAndSaveDigitalFootprint(startOfDay)
        }

        when (type) {
            "MORNING" -> {
                val hasEnergyLog = repository.hasLoggedEnergyToday(startOfDay).firstOrNull() ?: false
                if (!hasEnergyLog) {
                    showNotification(
                        "Morning Reminder",
                        "Did you remember to log your electricity/AC usage for today?",
                        101
                    )
                }
            }
            "EVENING" -> {
                val hasFoodLog = repository.hasLoggedFoodToday(startOfDay).firstOrNull() ?: false
                if (!hasFoodLog) {
                    showNotification(
                        "Evening Summary",
                        "How was your diet today? Don't forget to log your meals!",
                        102
                    )
                }
            }
        }

        return Result.success()
    }

    private suspend fun fetchAndSaveDigitalFootprint(startOfDay: Long) {
        val usageStatsManager = applicationContext.getSystemService(android.content.Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = startOfDay
        
        val stats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        if (!stats.isNullOrEmpty()) {
            val totalTimeMs = stats.values.sumOf { it.totalTimeInForeground }
            val minutes = totalTimeMs / (60 * 1000)
            
            // CO2 Calculation: ~0.036kg per hour base
            val co2Kg = (minutes / 60.0) * 0.036
            
            repository.saveDigitalLog(
                com.example.theglobalcarbonfootprintproject.data.local.entities.DigitalLog(
                    date = endTime,
                    screenTimeMinutes = minutes,
                    co2Kg = co2Kg
                )
            )
        }
    }

    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun showNotification(title: String, message: String, id: Int) {
        val channelId = "smart_logs"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Reminders", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }

    companion object {
        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)

            // Morning at 9 AM
            val morningRequest = PeriodicWorkRequestBuilder<SmartLogWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(calculateDelay(9), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("notification_type" to "MORNING"))
                .build()

            // Mid-day at 2 PM for digital and transport sync
            val midDayRequest = PeriodicWorkRequestBuilder<SmartLogWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(calculateDelay(14), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("notification_type" to "MID_DAY"))
                .build()

            // Evening at 8 PM
            val eveningRequest = PeriodicWorkRequestBuilder<SmartLogWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(calculateDelay(20), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("notification_type" to "EVENING"))
                .build()

            workManager.enqueueUniquePeriodicWork("morning_log", ExistingPeriodicWorkPolicy.KEEP, morningRequest)
            workManager.enqueueUniquePeriodicWork("mid_day_log", ExistingPeriodicWorkPolicy.KEEP, midDayRequest)
            workManager.enqueueUniquePeriodicWork("evening_log", ExistingPeriodicWorkPolicy.KEEP, eveningRequest)
        }

        private fun calculateDelay(targetHour: Int): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }
}

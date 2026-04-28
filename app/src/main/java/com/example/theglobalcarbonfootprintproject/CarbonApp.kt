package com.example.theglobalcarbonfootprintproject

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.theglobalcarbonfootprintproject.workers.SmartLogWorker
import com.example.theglobalcarbonfootprintproject.workers.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import io.realm.kotlin.mongodb.App
import javax.inject.Inject
import androidx.work.*
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class CarbonApp : Application(), Configuration.Provider {
    companion object {
        lateinit var realmApp: App
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        // WorkManager initialization is now handled by the Configuration.Provider
        createNotificationChannel()
        initRealm()
        SmartLogWorker.schedule(this)
        scheduleSync()
    }

    private fun scheduleSync() {
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "remote_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun initRealm() {
        // realmApp = App.create("carbon-app-xxxxx") // Replace with your actual App ID
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Activity Tracking"
            val descriptionText = "Monitoring transport for carbon footprint"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel("activity_tracking", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}

package com.example.theglobalcarbonfootprintproject

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.theglobalcarbonfootprintproject.data.local.entities.*
import dagger.hilt.android.HiltAndroidApp
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.mongodb.App
import io.realm.kotlin.mongodb.sync.SyncConfiguration

@HiltAndroidApp
class CarbonApp : Application() {
    companion object {
        lateinit var realmApp: App
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initRealm()
    }

    private fun initRealm() {
        realmApp = App.create("carbon-app-xxxxx") // Replace with your actual App ID
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

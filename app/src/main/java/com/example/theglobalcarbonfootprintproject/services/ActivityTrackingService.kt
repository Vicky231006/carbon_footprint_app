package com.example.theglobalcarbonfootprintproject.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.theglobalcarbonfootprintproject.MainActivity
import com.example.theglobalcarbonfootprintproject.R
import com.example.theglobalcarbonfootprintproject.receivers.ActivityTransitionReceiver
import com.google.android.gms.location.*

class ActivityTrackingService : Service() {

    private lateinit var activityClient: ActivityRecognitionClient
    private lateinit var pendingIntent: PendingIntent

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        activityClient = ActivityRecognition.getClient(this)
        
        val intent = Intent(this, ActivityTransitionReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        registerTransitions()
        registerStepCounter()
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun registerTransitions() {
        val transitions = listOf(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )

        val request = ActivityTransitionRequest(transitions)
        activityClient.requestActivityTransitionUpdates(request, pendingIntent)
            .addOnFailureListener { e ->
                // Handle failure
            }
    }

    private fun registerStepCounter() {
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepCounterSensor?.let {
            sensorManager.registerListener(stepListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val totalSteps = event.values[0].toInt()
            val prefs = getSharedPreferences("carbon_prefs", MODE_PRIVATE)
            
            // If it's the first time today, set baseline
            val lastResetDate = prefs.getLong("last_step_reset_date", 0L)
            val today = System.currentTimeMillis() / (24 * 60 * 60 * 1000)
            
            if (lastResetDate != today) {
                prefs.edit()
                    .putInt("step_baseline_today", totalSteps)
                    .putLong("last_step_reset_date", today)
                    .apply()
            }
            
            val baselineSteps = prefs.getInt("step_baseline_today", totalSteps)
            val stepsToday = totalSteps - baselineSteps
            prefs.edit().putInt("steps_today", stepsToday).apply()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Carbon Tracking Active")
            .setContentText("Automatically tracking your transport emissions")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // Use system icon for now
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        activityClient.removeActivityTransitionUpdates(pendingIntent)
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.unregisterListener(stepListener)
        super.onDestroy()
    }

    companion object {
        const val NOTIF_ID = 1001
        const val CHANNEL_ID = "activity_tracking"
    }
}

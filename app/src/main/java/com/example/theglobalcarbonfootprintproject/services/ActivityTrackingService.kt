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
            
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val lastResetDate = prefs.getString("step_reset_date", "")
            
            // 1. Update the total sensor reading so Dashboard has it for baseline resets
            prefs.edit().putInt("total_steps_sensor", totalSteps).apply()

            // 2. Handle Daily Reset
            if (lastResetDate != today) {
                prefs.edit()
                    .putInt("step_baseline_today", totalSteps)
                    .putString("step_reset_date", today)
                    .putInt("steps_today", 0)
                    .apply()
            }
            
            var baselineSteps = prefs.getInt("step_baseline_today", -1)
            
            // 3. Handle First Run / Invalid Baseline
            // If baseline is -1 or 0 (and totalSteps is large), it means we don't have a start-of-day baseline.
            // We should treat the current totalSteps as the baseline for today.
            if (baselineSteps <= 0 && totalSteps > 500) {
                prefs.edit().putInt("step_baseline_today", totalSteps).apply()
                baselineSteps = totalSteps
            } else if (baselineSteps == -1) {
                // If it's truly the first ever reading and totalSteps is small
                prefs.edit().putInt("step_baseline_today", totalSteps).apply()
                baselineSteps = totalSteps
            }
            
            // 4. Handle Device Reboots (where totalSteps < baselineSteps)
            var stepsToday = totalSteps - baselineSteps
            if (stepsToday < 0) {
                // Sensor was reset by system. Recalibrate baseline.
                prefs.edit().putInt("step_baseline_today", totalSteps).apply()
                stepsToday = 0
            }

            // 5. Final safety check: if it's still weirdly high (e.g. > 50k in one jump)
            // and we just started, it's likely a bad baseline.
            if (stepsToday > 40000 && baselineSteps == 0) {
                 prefs.edit().putInt("step_baseline_today", totalSteps).apply()
                 stepsToday = 0
            }

            // 4. Final safety check: if it's still weirdly high (e.g. > 50k in one jump), 
            // something is wrong with baseline. But for now, just save it.
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

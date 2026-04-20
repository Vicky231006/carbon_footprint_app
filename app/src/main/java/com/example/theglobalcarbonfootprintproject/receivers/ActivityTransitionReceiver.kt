package com.example.theglobalcarbonfootprintproject.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.theglobalcarbonfootprintproject.data.local.CarbonDatabase
import com.example.theglobalcarbonfootprintproject.data.local.entities.TransportSegment
import com.example.theglobalcarbonfootprintproject.calculator.TransportMode
import com.example.theglobalcarbonfootprintproject.calculator.TransportCalculator
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ActivityTransitionReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return

        for (event in result.transitionEvents) {
            val activityType = event.activityType
            val transition = event.transitionType

            when (transition) {
                ActivityTransition.ACTIVITY_TRANSITION_ENTER -> {
                    val prefs = context.getSharedPreferences("carbon_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putInt("active_activity_type", activityType)
                        .putLong("active_segment_start", System.currentTimeMillis())
                        .apply()
                }
                ActivityTransition.ACTIVITY_TRANSITION_EXIT -> {
                    val prefs = context.getSharedPreferences("carbon_prefs", Context.MODE_PRIVATE)
                    val startTime = prefs.getLong("active_segment_start", 0L)
                    if (startTime == 0L) return

                    val durationMinutes = (System.currentTimeMillis() - startTime) / 60_000.0
                    
                    val avgSpeedKmh = when (activityType) {
                        DetectedActivity.IN_VEHICLE -> 30.0
                        DetectedActivity.ON_BICYCLE -> 12.0
                        DetectedActivity.WALKING -> 5.0
                        DetectedActivity.RUNNING -> 10.0
                        else -> 0.0
                    }
                    val estimatedKm = (durationMinutes / 60.0) * avgSpeedKmh

                    val mode = when (activityType) {
                        DetectedActivity.IN_VEHICLE -> TransportMode.CAR // Default to Car, user can disambiguate
                        else -> TransportMode.WALK_BIKE
                    }

                    val co2Kg = TransportCalculator.calculate(estimatedKm, mode)

                    val segment = TransportSegment(
                        date = System.currentTimeMillis(),
                        activityType = activityType,
                        durationMinutes = durationMinutes,
                        estimatedKm = estimatedKm,
                        transportMode = mode,
                        co2Kg = co2Kg
                    )

                    scope.launch {
                        val db = CarbonDatabase.getDatabase(context)
                        db.transportDao().insert(segment)
                    }
                    prefs.edit().remove("active_segment_start").apply()
                }
            }
        }
    }
}

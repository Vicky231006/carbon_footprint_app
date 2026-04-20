package com.example.theglobalcarbonfootprintproject.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.theglobalcarbonfootprintproject.calculator.TransportMode

@Entity(tableName = "transport_segments")
data class TransportSegment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val activityType: Int,           // DetectedActivity constant
    val durationMinutes: Double,
    val estimatedKm: Double,
    val transportMode: TransportMode,
    val co2Kg: Double,
    val userVerified: Boolean = false // true if user manually confirmed mode
)

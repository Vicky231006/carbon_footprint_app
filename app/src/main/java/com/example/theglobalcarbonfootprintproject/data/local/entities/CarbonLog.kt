package com.example.theglobalcarbonfootprintproject.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "carbon_logs")
data class CarbonLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,             // System.currentTimeMillis()
    val transportKg: Double,
    val electricityKg: Double,
    val digitalKg: Double,
    val foodKg: Double,
    val wasteKg: Double = 0.0,
    val eventKg: Double = 0.0,
    val totalKg: Double,
    val greenPoints: Int        // earned this day
)

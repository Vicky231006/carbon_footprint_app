package com.example.theglobalcarbonfootprintproject.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "digital_logs")
data class DigitalLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val screenTimeMinutes: Long,
    val systemTimeMinutes: Long = 0,
    val co2Kg: Double
)

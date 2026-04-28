package com.example.theglobalcarbonfootprintproject.data.remote

data class DailyLogRequest(
    val deviceId: String,
    val date: String,
    val userType: String,
    val transportKg: Double,
    val energyKg: Double,
    val foodKg: Double,
    val digitalKg: Double,
    val wasteKg: Double = 0.0,
    val eventKg: Double = 0.0,
    val totalKg: Double,
    val score: Int,
    val kmWalked: Double,
    val stepsCount: Int,
    val energyLogged: Boolean,
    val foodLogged: Boolean,
    val transportAutoDetected: Boolean,
    val electricityMethod: String,
    val seasonLabel: String
)

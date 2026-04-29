package com.example.theglobalcarbonfootprintproject.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ACSeasonality { NONE, SEASONAL, YEAR_ROUND }

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val userType: String,       // "INDIVIDUAL" | "INSTITUTION"
    val dietType: String,
    val travelMode: String,
    val fuelType: String = "PETROL",
    val kmPerDay: Double = 10.0,
    val mealsPerDay: Int = 3,
    val screenTimeCategory: String,  // "light" | "moderate" | "heavy"
    val deviceCount: Int = 2,
    val streamingHeavy: Boolean = false,
    val acUsage: Boolean,
    val monthlyKwhBase: Double,
    val acSeasonality: ACSeasonality,
    val gridFactor: Double = 0.85,
    val onboardingComplete: Boolean = false,
    val state: String = "",
    val baseline_co2_daily: Double = 0.0
)



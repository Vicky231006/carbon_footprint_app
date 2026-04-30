package com.example.theglobalcarbonfootprintproject.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ACSeasonality { NONE, SEASONAL, YEAR_ROUND }

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val userType: String = "INDIVIDUAL",       // "INDIVIDUAL" | "INSTITUTION"
    val dietType: String? = "VEGETARIAN",
    val travelMode: String? = "CAR",
    val fuelType: String = "PETROL",
    val kmPerDay: Double = 10.0,
    val mealsPerDay: Int = 3,
    val screenTimeCategory: String? = "moderate",  // "light" | "moderate" | "heavy"
    val deviceCount: Int = 2,
    val streamingHeavy: Boolean = false,
    val acUsage: Boolean = false,
    val monthlyKwhBase: Double = 0.0,
    val acSeasonality: ACSeasonality? = ACSeasonality.NONE,
    val gridFactor: Double = 0.85,
    val onboardingComplete: Boolean = false,
    val state: String = "",
    val baseline_co2_daily: Double = 0.0
)




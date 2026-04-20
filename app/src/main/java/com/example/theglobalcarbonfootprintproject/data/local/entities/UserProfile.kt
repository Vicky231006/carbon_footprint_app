package com.example.theglobalcarbonfootprintproject.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val userType: String,       // "individual" | "institution"
    val dietType: String,
    val travelMode: String,
    val acUsage: Boolean,
    val monthlyBill: Double?,
    val screenTimeCategory: String  // "light" | "moderate" | "heavy"
)

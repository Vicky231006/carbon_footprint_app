package com.example.theglobalcarbonfootprintproject.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reward_records")
data class RewardRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val pointsEarned: Int,
    val reason: String          // "daily_log" | "streak" | "tip_adopted"
)

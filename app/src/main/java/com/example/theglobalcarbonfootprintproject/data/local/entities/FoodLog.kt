package com.example.theglobalcarbonfootprintproject.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_logs")
data class FoodLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val mealType: MealType,
    val co2Kg: Double,
    val description: String = ""
)

enum class MealType {
    HIGH_MEAT,
    LOW_MEAT,
    VEGETARIAN,
    VEGAN
}

package com.example.theglobalcarbonfootprintproject.calculator

enum class DietType {
    VEGAN, VEGETARIAN, MIXED, NON_VEG
}

object FoodCalculator {
    fun calculate(diet: DietType, mealsToday: Int = 3): Double {
        val perMeal = when(diet) {
            DietType.VEGAN       -> 0.5
            DietType.VEGETARIAN  -> 0.7
            DietType.MIXED       -> 1.2
            DietType.NON_VEG     -> 2.5
        }
        return perMeal * mealsToday
    }
}

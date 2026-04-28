package com.example.theglobalcarbonfootprintproject.calculator

import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.ACUsage
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.CookingType
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.DietType
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.FuelType
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.OnboardingData
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.ScreenTime
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.TransportMode

object CarbonEngine {
    fun calculateBaseline(data: OnboardingData): Double {
        val breakdown = calculateBreakdown(data)
        return breakdown.values.sum()
    }

    fun calculateBreakdown(data: OnboardingData): Map<String, Double> {
        val transport = calculateTransport(data.kmPerDay, data.primaryMode, data.fuelType)
        val electricity = SeasonalElectricityCalculator.getDailyCo2(
            baseKwh = data.monthlyKwhBase,
            seasonality = data.acSeasonality,
            gridFactor = data.gridFactor
        )
        val food = calculateFood(data.dietType, data.mealsPerDay)
        val digital = calculateDigital(
            screenCategory = data.screenTimeCategory,
            deviceCount = data.deviceCount,
            streamingHeavy = data.streamingHeavy
        )
        return mapOf(
            "Transport" to transport,
            "Energy" to electricity,
            "Food" to food,
            "Digital" to digital
        )
    }

    private fun estimateKwhFromAC(usage: ACUsage): Double = when (usage) {
        ACUsage.NONE -> 2.0   // kWh/day baseline Indian home
        ACUsage.OCCASIONAL -> 4.5
        ACUsage.DAILY -> 9.0
    }

    private fun calculateTransport(km: Double, mode: TransportMode, fuel: FuelType): Double {
        val factor = when (mode) {
            TransportMode.WALK_BIKE -> 0.0
            TransportMode.METRO -> 0.041
            TransportMode.BUS -> 0.089
            TransportMode.TWO_WHEELER -> if (fuel == FuelType.ELECTRIC) 0.012 else 0.065
            TransportMode.CAR -> when (fuel) {
                FuelType.PETROL -> 0.210
                FuelType.DIESEL -> 0.174
                FuelType.ELECTRIC -> 0.053
                else -> 0.210
            }
            TransportMode.PUBLIC_TRANSPORT -> 0.065 // Simplified average for generic public transport
            TransportMode.MIXED -> 0.12 // Average weighted
        }
        return km * factor
    }

    private fun calculateFood(type: DietType, meals: Int): Double {
        val factor = when (type) {
            DietType.VEGAN -> 0.50
            DietType.VEGETARIAN -> 0.70
            DietType.MIXED -> 1.20
            DietType.MEAT_HEAVY -> 2.50
        }
        return factor * meals
    }

    private fun calculateDigital(screenCategory: ScreenTime, deviceCount: Int, streamingHeavy: Boolean): Double {
        val baseHours = when (screenCategory) {
            ScreenTime.LIGHT -> 1.5
            ScreenTime.MODERATE -> 3.5
            ScreenTime.HEAVY -> 6.5
        }
        val perHourBase = 0.036
        val streamingMultiplier = if (streamingHeavy) 1.20 else 1.0
        val standby = deviceCount * 0.05
        return (baseHours * perHourBase * streamingMultiplier) + standby
    }
}

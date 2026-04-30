package com.example.theglobalcarbonfootprintproject.calculator

import com.example.theglobalcarbonfootprintproject.data.local.entities.ACSeasonality
import java.util.Calendar

object SeasonalElectricityCalculator {

    // Summer (high AC): March–June     → months 3,4,5,6
    // Monsoon (moderate): July–Sept    → months 7,8,9
    // Winter (low AC, maybe heater): Oct–Feb → months 10,11,12,1,2

    fun getMonthlyKwh(baseKwh: Double, seasonality: ACSeasonality?, month: Int = getCurrentMonth()): Double {
        val s = seasonality ?: ACSeasonality.NONE
        val multiplier = when (s) {
            ACSeasonality.NONE -> 1.0
            ACSeasonality.SEASONAL -> when (month) {
                in 3..6 -> 1.45   // summer — AC heavy
                in 7..9 -> 1.10   // monsoon — moderate
                else -> 0.75      // winter — low usage
            }
            ACSeasonality.YEAR_ROUND -> when (month) {
                in 3..6 -> 1.35
                in 7..9 -> 1.10
                else -> 0.90
            }
        }
        return baseKwh * multiplier
    }

    fun getDailyKwh(baseKwh: Double, seasonality: ACSeasonality?, month: Int = getCurrentMonth()): Double {
        val cal = Calendar.getInstance()
        if (month != getCurrentMonth()) {
            cal.set(Calendar.MONTH, month - 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH).toDouble()
        return getMonthlyKwh(baseKwh, seasonality, month) / daysInMonth
    }

    fun getDailyCo2(baseKwh: Double, seasonality: ACSeasonality?, gridFactor: Double, month: Int = getCurrentMonth()): Double {
        return getDailyKwh(baseKwh, seasonality, month) * gridFactor
    }


    fun getSeasonLabel(month: Int = getCurrentMonth()): String {
        return when (month) {
            in 3..6 -> "Summer estimate (AC season)"
            in 7..9 -> "Monsoon estimate"
            else -> "Winter estimate"
        }
    }

    private fun getCurrentMonth(): Int {
        return Calendar.getInstance().get(Calendar.MONTH) + 1 // 1-based
    }
}

package com.example.theglobalcarbonfootprintproject.calculator

object DigitalCalculator {
    fun calculate(screenHours: Double): Double {
        // 0.036 kg CO₂/hour (average smartphone + network)
        return screenHours * 0.036
    }
}

package com.example.theglobalcarbonfootprintproject.calculator

object ElectricityCalculator {
    const val INDIA_GRID_FACTOR = 0.82  // kg CO₂/kWh

    fun fromBill(billInRupees: Double): Double {
        val kWh = billInRupees / 8.0      // ~₹8/unit average India
        return kWh * INDIA_GRID_FACTOR
    }

    fun fromKwh(kWh: Double) = kWh * INDIA_GRID_FACTOR
}

package com.example.theglobalcarbonfootprintproject.calculator

import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.FuelType
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.TransportMode

object TransportConstants {

    const val BUS_KM_PER_DAY = 150.0 // Average km per bus per day for institution fleet

    fun emissionFactor(mode: TransportMode, fuel: FuelType? = null): Double = when (mode) {
        TransportMode.WALK_BIKE -> 0.0
        TransportMode.METRO -> 0.041 // kg CO2 per km
        TransportMode.BUS -> 0.089 // kg CO2 per km
        TransportMode.PUBLIC_TRANSPORT -> 0.089 // Treat public bus/auto like a bus for now
        TransportMode.TWO_WHEELER -> if (fuel == FuelType.ELECTRIC) 0.012 else 0.065
        TransportMode.CAR -> when (fuel) {
            FuelType.PETROL -> 0.210
            FuelType.DIESEL -> 0.174
            FuelType.ELECTRIC -> 0.053
            else -> 0.210
        }
        TransportMode.MIXED -> 0.12 // Average weighted
    }
}

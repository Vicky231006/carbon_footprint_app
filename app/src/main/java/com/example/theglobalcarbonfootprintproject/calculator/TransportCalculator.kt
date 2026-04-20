package com.example.theglobalcarbonfootprintproject.calculator

enum class TransportMode {
    CAR, BUS, METRO, WALK_BIKE, FLIGHT
}

object TransportCalculator {
    fun calculate(km: Double, mode: TransportMode): Double {
        val factor = when (mode) {
            TransportMode.CAR -> 0.21   // kg CO₂/km
            TransportMode.BUS -> 0.089
            TransportMode.METRO -> 0.041
            TransportMode.WALK_BIKE -> 0.0
            TransportMode.FLIGHT -> 0.255
        }
        return km * factor
    }
}

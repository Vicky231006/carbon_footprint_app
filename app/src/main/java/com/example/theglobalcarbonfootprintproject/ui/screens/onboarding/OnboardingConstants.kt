package com.example.theglobalcarbonfootprintproject.ui.screens.onboarding

import androidx.compose.ui.unit.dp

object OnboardingSpacing {
    val screenHorizontal = 24.dp
    val sectionGap = 28.dp
    val itemGap = 12.dp
    val cardGap = 10.dp
    val progressTopPad = 16.dp
    val illustrationSize = 160.dp
    val buttonHeight = 56.dp
    val buttonBottom = 24.dp
}

object LocationConstants {
    val INDIA_GRID_FACTORS = mapOf(
        "Maharashtra" to 0.82,
        "Karnataka" to 0.72,
        "Delhi" to 0.71,
        "Tamil Nadu" to 0.81,
        "West Bengal" to 0.91,
        "Uttar Pradesh" to 0.94,
        "Gujarat" to 0.79,
        "Rajasthan" to 0.76,
        "Andhra Pradesh" to 0.74,
        "Telangana" to 0.83,
        "Madhya Pradesh" to 0.88,
        "Bihar" to 0.96,
        "Punjab" to 0.69,
        "Haryana" to 0.77,
        "Kerala" to 0.41,
        "Himachal Pradesh" to 0.10,
        "Uttarakhand" to 0.15,
        "Goa" to 0.75,
    ).withDefault { 0.82 }
}

object TransportConstants {
    // kg CO2 per km
    const val WALK_BIKE = 0.000
    const val METRO = 0.041
    const val BUS = 0.089
    const val TWO_WHEELER_PETROL = 0.065
    const val TWO_WHEELER_ELECTRIC = 0.012
    const val CAR_PETROL = 0.210
    const val CAR_DIESEL = 0.174
    const val CAR_ELECTRIC = 0.053
}

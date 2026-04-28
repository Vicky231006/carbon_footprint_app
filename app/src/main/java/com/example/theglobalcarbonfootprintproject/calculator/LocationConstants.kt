package com.example.theglobalcarbonfootprintproject.calculator

object LocationConstants {
    // CO2 emission factor for electricity grid in kg CO2/kWh
    // Data based on CEA (Central Electricity Authority) reports for Indian States
    val INDIA_GRID_FACTORS = mapOf(
        "Andhra Pradesh" to 0.81,
        "Arunachal Pradesh" to 0.35, // High hydro share
        "Assam" to 0.72,
        "Bihar" to 0.90,
        "Chhattisgarh" to 0.95,
        "Goa" to 0.82,
        "Gujarat" to 0.84,
        "Haryana" to 0.88,
        "Himachal Pradesh" to 0.15, // Mostly hydro
        "Jharkhand" to 0.92,
        "Karnataka" to 0.70, // Significant RE share
        "Kerala" to 0.55,
        "Madhya Pradesh" to 0.87,
        "Maharashtra" to 0.83,
        "Manipur" to 0.45,
        "Meghalaya" to 0.30,
        "Mizoram" to 0.32,
        "Nagaland" to 0.35,
        "Odisha" to 0.91,
        "Punjab" to 0.85,
        "Rajasthan" to 0.86,
        "Sikkim" to 0.10,
        "Tamil Nadu" to 0.78,
        "Telangana" to 0.82,
        "Tripura" to 0.65,
        "Uttar Pradesh" to 0.89,
        "Uttarakhand" to 0.25,
        "West Bengal" to 0.88,
        "Andaman and Nicobar Islands" to 0.95, // Diesel generation
        "Chandigarh" to 0.82,
        "Dadra and Nagar Haveli and Daman and Diu" to 0.82,
        "Delhi" to 0.82,
        "Jammu and Kashmir" to 0.40,
        "Ladakh" to 0.40,
        "Lakshadweep" to 0.98,
        "Puducherry" to 0.82,
        "India (Average)" to 0.82
    )
}

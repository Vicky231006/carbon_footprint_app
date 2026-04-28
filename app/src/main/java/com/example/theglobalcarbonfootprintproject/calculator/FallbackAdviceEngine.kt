package com.example.theglobalcarbonfootprintproject.calculator

import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.UserType

object FallbackAdviceEngine {

    private val individualTips = mapOf(
        "Transport" to listOf(
            "Consider carpooling or using public transport to reduce your travel footprint.",
            "Maintain your vehicle's tire pressure for better fuel efficiency.",
            "Try walking or cycling for short distances."
        ),
        "Energy" to listOf(
            "Switch to LED bulbs to save electricity.",
            "Unplug electronics when not in use to avoid phantom energy consumption.",
            "Use a programmable thermostat to optimize heating and cooling."
        ),
        "Food" to listOf(
            "Reduce meat consumption, especially red meat, which has a high carbon footprint.",
            "Buy local and seasonal produce to reduce food miles.",
            "Minimize food waste by planning meals and storing food properly."
        ),
        "Digital" to listOf(
            "Reduce screen time and engage in offline activities.",
            "Lower the brightness of your devices to save battery.",
            "Clean up your digital storage to reduce the energy used by data centers."
        )
    )

    private val institutionTips = mapOf(
        "Labs" to listOf(
            "Ensure PCs are turned off or put in sleep mode after lab hours.",
            "Implement a schedule for equipment usage to optimize energy consumption."
        ),
        "Fleet" to listOf(
            "Optimize bus routes to reduce total distance traveled.",
            "Regularly service the institution's vehicles for better fuel efficiency."
        ),
        "Infrastructure" to listOf(
            "Install motion-sensor lights in hallways and common areas.",
            "Consider installing solar panels to offset grid energy usage."
        )
    )

    fun getAdvice(userType: UserType, category: String? = null): String {
        val tips = if (userType == UserType.INDIVIDUAL) individualTips else institutionTips
        
        return if (category != null && tips.containsKey(category)) {
            tips[category]?.random() ?: "Stay eco-friendly!"
        } else {
            tips.values.flatten().random()
        }
    }
}

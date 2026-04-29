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

    private val hardcodedAnswers = mapOf(
        // Individual Questions
        "What are the best public transport alternatives to reduce emissions?" to 
            "Switching to public transport like Metros and AC buses is highly effective. In India, using the Metro can reduce your per-km footprint by up to 80% compared to driving a private car.",
        "How much CO₂ am I saving by using public transport?" to 
            "On average, using public transport in India saves approximately 150-200g of CO₂ per kilometer compared to a mid-sized petrol car. Over a year, this can save over a tonne of CO₂!",
        "What other eco-friendly habits can complement my walking lifestyle?" to 
            "Since you already walk, you can further lower your impact by choosing locally sourced seasonal food, using a reusable water bottle, and switching to energy-efficient LED lighting at home.",
        "How can I make my daily commute more eco-friendly?" to 
            "Consider carpooling with colleagues or neighbors. If driving, maintain steady speeds and ensure your vehicle is regularly serviced to optimize fuel efficiency and reduce emissions.",
        "What are the top 5 easy ways to reduce my carbon footprint at home?" to 
            "1. Switch to LED bulbs. 2. Set AC to 24°C. 3. Unplug electronics when not in use. 4. Shorten showers to save water heating energy. 5. Compost your kitchen waste.",
        "How does seasonal weather affect household energy consumption in India?" to 
            "Indian summers cause a significant spike in energy use due to air conditioning. Setting your AC to 24°C instead of 18°C and using ceiling fans can save up to 25% on your cooling bill.",
        "How does my digital screen time contribute to carbon emissions?" to 
            "Streaming HD video and cloud storage rely on energy-intensive data centers. To reduce this, lower your streaming resolution, delete old emails, and use Wi-Fi instead of mobile data.",

        // Institutional Questions
        "How can our institution transition to 100% renewable energy?" to 
            "Start with a rooftop solar audit. Institutions can often offset 30-50% of energy via solar. For the remaining 50%, look into green power purchase agreements (PPAs) with local utilities.",
        "What are the best waste management practices for large campus canteens?" to 
            "Implement on-site composting or a small-scale biogas plant for food waste. Eliminate single-use plastics in favor of reusable stainless steel or glass, and conduct regular waste audits.",
        "How does improving building insulation affect our carbon footprint?" to 
            "Reflective 'Cool Roof' paint and better window shading can reduce indoor temperatures by 3-5°C, lowering the energy required for air conditioning by 15-20% annually.",
        "Propose a green commuting policy for students and staff." to 
            "Incentivize carpooling with reserved parking, provide secure bicycle racks and showers, and consider transitioning your institution's bus fleet to Electric Vehicles (EVs) over time."
    )

    fun getAdvice(userType: UserType, category: String? = null): String {
        val tips = if (userType == UserType.INDIVIDUAL) individualTips else institutionTips
        
        return if (category != null && tips.containsKey(category)) {
            tips[category]?.random() ?: "Stay eco-friendly!"
        } else {
            tips.values.flatten().random()
        }
    }

    fun getHardcodedAnswer(question: String): String? {
        return hardcodedAnswers[question]
    }
}


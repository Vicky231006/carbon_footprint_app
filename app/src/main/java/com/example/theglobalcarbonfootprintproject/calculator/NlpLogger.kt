package com.example.theglobalcarbonfootprintproject.calculator

import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.UserType
import org.json.JSONObject

object NlpLogger {
    fun getSystemPrompt(userType: UserType): String {
        val basePrompt = """
            You are a Carbon Logging Assistant for an app called GreenView. 
            Convert the user's natural language input into a structured JSON object for logging their footprint.
            
            USER TYPE: ${userType.name}
            
            CATEGORIES:
            - FOOD: Fields: { "type": "VEGAN"|"VEGETARIAN"|"MIXED"|"MEAT_HEAVY", "meals": number }
            - TRANSPORT: Fields: { "mode": "CAR"|"BUS"|"METRO"|"TWO_WHEELER"|"WALK_BIKE", "km": number }
            - ENERGY: Fields: { "kwh": number }
            - WASTE (Institutional): Fields: { "kg": number }
            - EVENT (Institutional): Fields: { "attendees": number, "description": string }
            
            RULES:
            1. Return ONLY a JSON object. No chat or explanations.
            2. If specific numbers are missing, use reasonable defaults: meals=1, km=5, kwh=2.
            3. If the input is ambiguous, map it to the closest category.
            
            EXAMPLES:
            "Ate a chicken burger" -> { "category": "FOOD", "type": "MEAT_HEAVY", "meals": 1 }
            "Took the metro for 10km" -> { "category": "TRANSPORT", "mode": "METRO", "km": 10 }
            "Electricity bill was 150 units" -> { "category": "ENERGY", "kwh": 150 }
            "Hosted a 100 person seminar" -> { "category": "EVENT", "attendees": 100, "description": "Seminar" }
            "Burger" -> { "category": "FOOD", "type": "MEAT_HEAVY", "meals": 1 }
            "Drove" -> { "category": "TRANSPORT", "mode": "CAR", "km": 5 }
        """.trimIndent()
        return basePrompt
    }

    data class ParsedLog(
        val category: String,
        val data: Map<String, Any>
    )

    fun parseResponse(jsonStr: String): ParsedLog? {
        return try {
            val cleanJson = jsonStr.substringAfter("{").substringBeforeLast("}") 
            val json = JSONObject("{$cleanJson}")
            val category = json.getString("category")
            val dataMap = mutableMapOf<String, Any>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key != "category") {
                    dataMap[key] = json.get(key)
                }
            }
            ParsedLog(category, dataMap)
        } catch (e: Exception) {
            null
        }
    }

    fun tryLocalParse(input: String): ParsedLog? {
        val text = input.lowercase().trim()
        
        // 1. Simple Food Keywords
        if (text.contains("burger") || text.contains("meat") || text.contains("chicken") || text.contains("biryani") || text.contains("egg")) {
            return ParsedLog("FOOD", mapOf("type" to "MEAT_HEAVY", "meals" to 1))
        }
        if (text.contains("paneer") || text.contains("veg") || text.contains("milk") || text.contains("dosa") || text.contains("idli")) {
            return ParsedLog("FOOD", mapOf("type" to "VEGETARIAN", "meals" to 1))
        }
        if (text.contains("vegan") || text.contains("salad") || text.contains("fruit") || text.contains("oats")) {
            return ParsedLog("FOOD", mapOf("type" to "VEGAN", "meals" to 1))
        }

        // 2. Simple Transport Keywords
        if (text.contains("metro") || text.contains("train")) {
            return ParsedLog("TRANSPORT", mapOf("mode" to "METRO", "km" to 10))
        }
        if (text.contains("bus")) {
            return ParsedLog("TRANSPORT", mapOf("mode" to "BUS", "km" to 8))
        }
        if (text.contains("car") || text.contains("drive") || text.contains("drove")) {
            return ParsedLog("TRANSPORT", mapOf("mode" to "CAR", "km" to 5))
        }
        if (text.contains("walk") || text.contains("cycle") || text.contains("bike")) {
            return ParsedLog("TRANSPORT", mapOf("mode" to "WALK_BIKE", "km" to 2))
        }

        // 3. Simple Energy Keywords
        if (text.contains("units") || text.contains("kwh") || text.contains("electricity")) {
            val digits = text.filter { it.isDigit() }.toDoubleOrNull() ?: 5.0
            return ParsedLog("ENERGY", mapOf("kwh" to digits))
        }

        return null
    }
}


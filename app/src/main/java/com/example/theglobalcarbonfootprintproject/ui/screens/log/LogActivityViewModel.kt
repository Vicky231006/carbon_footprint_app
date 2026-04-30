package com.example.theglobalcarbonfootprintproject.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theglobalcarbonfootprintproject.data.local.entities.EnergyLog
import com.example.theglobalcarbonfootprintproject.data.local.entities.EnergyType
import com.example.theglobalcarbonfootprintproject.data.local.entities.FoodLog
import com.example.theglobalcarbonfootprintproject.data.local.entities.MealType
import com.example.theglobalcarbonfootprintproject.data.repository.CarbonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogActivityViewModel @Inject constructor(
    private val repository: CarbonRepository
) : ViewModel() {


    fun logFood(mealType: MealType, description: String) {
        viewModelScope.launch {
            val co2 = when (mealType) {
                MealType.HIGH_MEAT -> 2.5
                MealType.LOW_MEAT -> 1.5
                MealType.VEGETARIAN -> 0.8
                MealType.VEGAN -> 0.5
            }
            repository.saveFoodLog(
                FoodLog(
                    date = System.currentTimeMillis(),
                    mealType = mealType,
                    co2Kg = co2,
                    description = description
                )
            )
        }
    }

    fun logEnergy(energyType: EnergyType, value: Double) {
        viewModelScope.launch {
            val factor = when (energyType) {
                EnergyType.ELECTRICITY -> 0.85 // kg per kWh (approx India avg)
                EnergyType.NATURAL_GAS -> 2.0  // kg per unit
                EnergyType.LPG -> 2.98         // kg per kg
                EnergyType.COAL -> 2.42        // kg per kg
                EnergyType.GRID -> 0.82
                EnergyType.SOLAR -> 0.05
                EnergyType.DIESEL -> 2.68
            }
            repository.saveEnergyLog(
                EnergyLog(
                    date = System.currentTimeMillis(),
                    energyType = energyType,
                    value = value,
                    kwh = if (energyType == EnergyType.ELECTRICITY) value else 0.0,
                    co2Kg = value * factor
                )
            )
        }
    }
    private var generativeModel: com.google.ai.client.generativeai.GenerativeModel? = null
    private val _nlpLoading = kotlinx.coroutines.flow.MutableStateFlow(false)
    val nlpLoading = _nlpLoading.asStateFlow()

    private val _nlpResult = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val nlpResult = _nlpResult.asStateFlow()

    init {
        initNlpModel()
    }

    private fun initNlpModel() {
        val apiKey = com.example.theglobalcarbonfootprintproject.BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank()) {
            generativeModel = com.google.ai.client.generativeai.GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey,
                systemInstruction = com.google.ai.client.generativeai.type.content { 
                    text(com.example.theglobalcarbonfootprintproject.calculator.NlpLogger.getSystemPrompt(
                        com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.UserType.INDIVIDUAL
                    )) 
                }
            )
        }
    }

    fun processNlpInput(text: String) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            _nlpLoading.value = true
            
            // 1. Try Local Parse First (Avoid API calls for simple things)
            val localParsed = com.example.theglobalcarbonfootprintproject.calculator.NlpLogger.tryLocalParse(text)
            if (localParsed != null) {
                repository.saveAiParsedLog(localParsed)
                _nlpResult.value = "Success! Logged ${localParsed.category} (Local)"
                _nlpLoading.value = false
                return@launch
            }

            // 2. Fallback to Gemini if local fails
            if (generativeModel != null) {
                try {
                    val response = generativeModel!!.generateContent(text)
                    val jsonStr = response.text ?: ""
                    val parsed = com.example.theglobalcarbonfootprintproject.calculator.NlpLogger.parseResponse(jsonStr)
                    if (parsed != null) {
                        repository.saveAiParsedLog(parsed)
                        _nlpResult.value = "Success! Logged ${parsed.category} (AI)"
                    } else {
                        _nlpResult.value = "Couldn't understand that. Try being more specific!"
                    }
                } catch (e: Exception) {
                    _nlpResult.value = "Offline: Couldn't understand. Try manual log."
                }
            } else {
                _nlpResult.value = "Offline: Try using keywords like 'burger' or 'metro'."
            }
            _nlpLoading.value = false
        }
    }

    fun clearNlpResult() {
        _nlpResult.value = null
    }
}



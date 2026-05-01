package com.example.theglobalcarbonfootprintproject.ui.screens.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theglobalcarbonfootprintproject.BuildConfig
import com.example.theglobalcarbonfootprintproject.calculator.FallbackAdviceEngine
import com.example.theglobalcarbonfootprintproject.data.local.dao.CarbonDao
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.UserType
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = true,
    val suggestions: List<String> = emptyList(),
    val geminiConnected: Boolean = false
)

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val carbonDao: CarbonDao,
    private val sharedPreferences: android.content.SharedPreferences,
    private val apiService: com.example.theglobalcarbonfootprintproject.data.remote.MongoApiService
) : ViewModel() {


    private val _uiState = MutableStateFlow(AiUiState())
    val uiState = _uiState.asStateFlow()

    private var chatContext = ""

    init {
        initializeAssistant()
    }

    private fun initializeAssistant() {
        viewModelScope.launch {
            val userProfile = carbonDao.getUserProfile().first()
            val instProfile = carbonDao.getInstitutionProfile().first()
            val isInst = sharedPreferences.getString("user_type", "INDIVIDUAL") == "INSTITUTION"
            
            val name = if (isInst) instProfile?.name ?: "Campus Admin" else userProfile?.name ?: "Eco-warrior"

            // Build personalized tips
            val tips = mutableListOf<String>()
            if (isInst && instProfile != null) {
                if ((instProfile.solarCapacityKw) <= 0) tips.add("Transitioning to solar could offset a major part of your energy bill.")
                if (instProfile.paperReamsMonth > 500) tips.add("Digitizing office workflows could save ${instProfile.paperReamsMonth} reams of paper monthly.")
            } else if (userProfile != null) {
                if (userProfile.kmPerDay > 20 && userProfile.travelMode != "METRO") {
                    tips.add("Your commute is quite long! Consider carpooling or switching to the metro.")
                }
                if (userProfile.dietType == "MEAT_HEAVY") {
                    tips.add("Switching to one plant-based meal a day can drastically lower your footprint.")
                }
                if (userProfile.acUsage) {
                    tips.add("Set your AC to 24°C instead of 18°C to save energy.")
                }
            }

            if (tips.isEmpty()) {
                tips.add("Keep up the great work reducing your footprint!")
                tips.add("Try unplugging unused electronics.")
                tips.add("Carry a reusable water bottle.")
            }

            val suggestions = buildSuggestions(userProfile)

            val welcomeMessage = if (isInst && instProfile != null) {
                val solarText = if (instProfile.solarCapacityKw > 0) "Your ${instProfile.solarCapacityKw}kW solar setup is fantastic!" else "Have you considered solar power for the campus?"
                "Hello $name! I am your Campus Sustainability Consultant. $solarText Currently, your daily energy footprint is approximately ${String.format(java.util.Locale.US, "%.1f", instProfile.monthlyEnergyKwh / 30.0)} kWh. How can I help you optimize today?"
            } else {
                val tipText = tips.joinToString("\n- ")
                "Hello $name! Here are some personalized tips based on your profile:\n- $tipText\n\nTap a suggestion below or ask me anything!"
            }

            _uiState.value = AiUiState(
                messages = listOf(ChatMessage(welcomeMessage, false)),
                isLoading = false,
                suggestions = suggestions,
                geminiConnected = true 
            )

            // Define Persona Context
            chatContext = if (isInst) {
                "You are a specialized Institutional Carbon Consultant for an Indian campus/organization named $name. " +
                "Provide strategic, data-driven advice on reducing the carbon footprint of large facilities. " +
                "Context: State: ${instProfile?.state}, Student Count: ${instProfile?.studentCount}, Energy: ${instProfile?.monthlyEnergyKwh} kWh/mo. " +
                "Focus on: HVAC optimization, solar transition, large-scale waste management, " +
                "and sustainable procurement. Keep responses professional and actionable."
            } else {
                "You are a highly helpful Carbon Footprint Assistant for an Indian user named $name. " +
                "Context: State: ${userProfile?.state}, Commute: ${userProfile?.kmPerDay} km. " +
                "Provide practical, encouraging, and specific advice on reducing carbon footprint. " +
                "Focus on Indian context. Keep responses concise (2-3 paragraphs max)."
            }
        }
    }


    private fun buildSuggestions(profile: com.example.theglobalcarbonfootprintproject.data.local.entities.UserProfile?): List<String> {
        val isInst = sharedPreferences.getString("user_type", "INDIVIDUAL") == "INSTITUTION"
        val suggestions = mutableListOf<String>()

        if (isInst) {
            suggestions.add("How can our institution transition to 100% renewable energy?")
            suggestions.add("What are the best waste management practices for large campus canteens?")
            suggestions.add("How does improving building insulation affect our carbon footprint?")
            suggestions.add("Propose a green commuting policy for students and staff.")
        } else {
            // 1. Transport — based on mode
            val mode = profile?.travelMode?.uppercase() ?: "CAR"
            suggestions.add(when (mode) {
                "CAR", "TWO_WHEELER" -> "What are the best public transport alternatives to reduce emissions?"
                "METRO", "BUS", "PUBLIC_TRANSPORT" -> "How much CO₂ am I saving by using public transport?"
                "WALK_BIKE" -> "What other eco-friendly habits can complement my walking lifestyle?"
                else -> "How can I make my daily commute more eco-friendly?"
            })

            // 2. General eco tip
            suggestions.add("What are the top 5 easy ways to reduce my carbon footprint at home?")

            // 3. Seasonal/general energy tip
            suggestions.add("How does seasonal weather affect household energy consumption in India?")

            // 4. Lifestyle/awareness
            suggestions.add("How does my digital screen time contribute to carbon emissions?")
        }

        return suggestions
    }


    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(ChatMessage(text, true))
        _uiState.value = _uiState.value.copy(messages = currentMessages, isLoading = true, suggestions = emptyList())

        viewModelScope.launch {
            try {
                val response = apiService.chatWithAi(
                    com.example.theglobalcarbonfootprintproject.data.remote.ChatRequest(
                        message = text,
                        context = chatContext
                    )
                )

                if (response.success && response.reply != null) {
                    val updatedMessages = _uiState.value.messages.toMutableList()
                    updatedMessages.add(ChatMessage(response.reply, false))
                    _uiState.value = _uiState.value.copy(messages = updatedMessages, isLoading = false, geminiConnected = true)
                } else {
                    throw Exception(response.error ?: "Failed to get response from server")
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error"
                Log.e("AiAssistant", "Backend AI error: $errorMsg")

                val hardcoded = FallbackAdviceEngine.getHardcodedAnswer(text)
                val userType = if (sharedPreferences.getString("user_type", "INDIVIDUAL") == "INSTITUTION") UserType.INSTITUTION else UserType.INDIVIDUAL
                val fallbackTip = hardcoded ?: FallbackAdviceEngine.getAdvice(userType)

                val updatedMessages = _uiState.value.messages.toMutableList()
                updatedMessages.add(ChatMessage("[Service Syncing] $fallbackTip", false))
                _uiState.value = _uiState.value.copy(messages = updatedMessages, isLoading = false, geminiConnected = false)
            }
        }
    }
}

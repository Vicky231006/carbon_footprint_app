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
    private val sharedPreferences: android.content.SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState = _uiState.asStateFlow()

    private var generativeModel: GenerativeModel? = null
    private var chatContext = ""

    init {
        initializeAssistant()
    }

    private fun initializeAssistant() {
        viewModelScope.launch {
            val userProfile = carbonDao.getUserProfile().first()
            val name = userProfile?.name ?: "Eco-warrior"

            // Build personalized tips from profile
            val tips = mutableListOf<String>()
            if (userProfile != null) {
                if (userProfile.kmPerDay > 20 && userProfile.travelMode != "METRO") {
                    tips.add("Your commute is quite long! Consider carpooling or switching to the metro.")
                }
                if (userProfile.dietType == "MEAT_HEAVY") {
                    tips.add("Switching to one plant-based meal a day can drastically lower your footprint.")
                }
                if (userProfile.acUsage) {
                    tips.add("Set your AC to 24°C instead of 18°C to save energy.")
                }
                if (userProfile.streamingHeavy) {
                    tips.add("Download videos on Wi-Fi instead of streaming over mobile data.")
                }
            }

            if (tips.isEmpty()) {
                tips.add("Keep up the great work reducing your footprint!")
                tips.add("Try unplugging unused electronics.")
                tips.add("Carry a reusable water bottle.")
            }

            // Generate 4 generic suggestion chips (not targeted per user feedback)
            val suggestions = buildSuggestions(userProfile)

            val tipText = tips.joinToString("\n- ")
            val welcomeMessage = "Hello $name! Here are some personalized tips based on your profile:\n- $tipText\n\nTap a suggestion below or ask me anything!"

            _uiState.value = AiUiState(
                messages = listOf(ChatMessage(welcomeMessage, false)),
                isLoading = false,
                suggestions = suggestions,
                geminiConnected = false
            )

            // Verify Gemini API connection
            val isInst = sharedPreferences.getString("user_type", "INDIVIDUAL") == "INSTITUTION"
            chatContext = if (isInst) {
                "You are a specialized Institutional Carbon Consultant for an Indian campus/organization. " +
                "Provide strategic, data-driven advice on reducing the carbon footprint of large facilities. " +
                "Focus on: HVAC optimization, solar transition, large-scale waste management (biogas/composting), " +
                "and sustainable procurement. Keep responses professional and actionable for campus administrators."
            } else {
                "You are a highly helpful Carbon Footprint Assistant for an Indian user. The user's name is $name. " +
                "Provide practical, encouraging, and specific advice on reducing carbon footprint. " +
                "Focus on Indian context: public transport like metros and buses, seasonal electricity with ACs, " +
                "vegetarian vs non-vegetarian diets, and digital footprint from streaming. " +
                "Keep responses concise (2-3 paragraphs max)."
            }


            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isNotBlank()) {
                try {
                    generativeModel = GenerativeModel(
                        modelName = "gemini-2.5-flash",
                        apiKey = apiKey,
                        systemInstruction = content { text(chatContext) }
                    )


                    // Verify connection with a quick test
                    val testResponse = generativeModel!!.generateContent("Say 'connected' in one word")
                    if (testResponse.text != null) {
                        _uiState.value = _uiState.value.copy(geminiConnected = true)
                        Log.d("AiAssistant", "Gemini API connected successfully")
                    }
                } catch (e: Exception) {
                    Log.e("AiAssistant", "Gemini API connection failed: ${e.message}")
                    generativeModel = null
                    _uiState.value = _uiState.value.copy(geminiConnected = false)
                }
            } else {
                Log.w("AiAssistant", "GEMINI_API_KEY is blank, running in offline mode")
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

            // 2. General eco tip (kept generic per user feedback)
            suggestions.add("What are the top 5 easy ways to reduce my carbon footprint at home?")

            // 3. Seasonal/general energy tip (generic, not targeted)
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
        // Hide suggestions after first interaction
        _uiState.value = _uiState.value.copy(messages = currentMessages, isLoading = true, suggestions = emptyList())

        viewModelScope.launch {
            if (generativeModel != null) {
                try {
                    val response = generativeModel!!.generateContent(text)
                    val aiResponse = response.text ?: "I'm sorry, I couldn't process that."

                    val updatedMessages = _uiState.value.messages.toMutableList()
                    updatedMessages.add(ChatMessage(aiResponse, false))
                    _uiState.value = _uiState.value.copy(messages = updatedMessages, isLoading = false)
                } catch (e: Exception) {
                    val errorMsg = e.message ?: "Unknown error"
                    Log.e("AiAssistant", "Gemini error: $errorMsg")
                    
                    if (errorMsg.contains("429") || errorMsg.contains("Quota")) {
                        Log.w("AiAssistant", "Rate limit hit! Switching to offline mode temporarily.")
                    }

                    val hardcoded = FallbackAdviceEngine.getHardcodedAnswer(text)

                    val userType = if (sharedPreferences.getString("user_type", "INDIVIDUAL") == "INSTITUTION") UserType.INSTITUTION else UserType.INDIVIDUAL
                    val fallbackTip = hardcoded ?: FallbackAdviceEngine.getAdvice(userType)
                    
                    val updatedMessages = _uiState.value.messages.toMutableList()
                    updatedMessages.add(ChatMessage("[Offline] $fallbackTip", false))
                    _uiState.value = _uiState.value.copy(messages = updatedMessages, isLoading = false)
                }
            } else {
                // Offline fallback
                val hardcoded = FallbackAdviceEngine.getHardcodedAnswer(text)
                val userType = if (sharedPreferences.getString("user_type", "INDIVIDUAL") == "INSTITUTION") UserType.INSTITUTION else UserType.INDIVIDUAL
                val fallbackTip = hardcoded ?: FallbackAdviceEngine.getAdvice(userType)
                
                val updatedMessages = _uiState.value.messages.toMutableList()
                updatedMessages.add(ChatMessage("[Offline] $fallbackTip", false))
                _uiState.value = _uiState.value.copy(messages = updatedMessages, isLoading = false)
            }
        }

    }
}

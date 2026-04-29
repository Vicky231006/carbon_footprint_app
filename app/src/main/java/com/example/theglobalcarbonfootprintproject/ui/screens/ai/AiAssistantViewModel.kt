package com.example.theglobalcarbonfootprintproject.ui.screens.ai

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
    val isLoading: Boolean = true
)

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val carbonDao: CarbonDao
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
            
            // Hardcoded Logic based on profile
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

            val tipText = tips.joinToString("\n- ")
            val welcomeMessage = "Hello $name! Here are some personalized tips based on your profile:\n- $tipText\n\nHow else can I help you today?"

            _uiState.value = AiUiState(messages = listOf(ChatMessage(welcomeMessage, false)), isLoading = false)

            chatContext = "You are a highly helpful Carbon Footprint Assistant. The user's name is $name. " +
                    "Here are 3-5 hardcoded tips we just gave them: $tipText. " +
                    "Use these tips to provide elaborated, personalized, and encouraging advice."

            generativeModel = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = BuildConfig.GEMINI_API_KEY,
                systemInstruction = content { text(chatContext) }
            )
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || generativeModel == null) return

        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(ChatMessage(text, true))
        _uiState.value = _uiState.value.copy(messages = currentMessages, isLoading = true)

        viewModelScope.launch {
            try {
                val response = generativeModel!!.generateContent(text)
                val aiResponse = response.text ?: "I'm sorry, I couldn't process that."
                
                val updatedMessages = _uiState.value.messages.toMutableList()
                updatedMessages.add(ChatMessage(aiResponse, false))
                _uiState.value = _uiState.value.copy(messages = updatedMessages, isLoading = false)
            } catch (e: Exception) {
                val fallbackTip = FallbackAdviceEngine.getAdvice(UserType.INDIVIDUAL)
                val updatedMessages = _uiState.value.messages.toMutableList()
                updatedMessages.add(ChatMessage("[Offline] $fallbackTip", false))
                _uiState.value = _uiState.value.copy(messages = updatedMessages, isLoading = false)
            }
        }
    }
}

package com.example.theglobalcarbonfootprintproject.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiUiState(
    val messages: List<ChatMessage> = listOf(ChatMessage("Hello! I'm your Eco AI Assistant. How can I help you reduce your carbon footprint today?", false)),
    val isLoading: Boolean = false
)

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@HiltViewModel
class AiAssistantViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(AiUiState())
    val uiState = _uiState.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(ChatMessage(text, true))
        _uiState.value = _uiState.value.copy(messages = currentMessages, isLoading = true)

        viewModelScope.launch {
            // Mock Gemini Response for now
            kotlinx.coroutines.delay(1500)
            val response = getMockResponse(text)
            val updatedMessages = _uiState.value.messages.toMutableList()
            updatedMessages.add(ChatMessage(response, false))
            _uiState.value = _uiState.value.copy(messages = updatedMessages, isLoading = false)
        }
    }

    private fun getMockResponse(input: String): String {
        return when {
            input.contains("meat", ignoreCase = true) -> "Reducing meat consumption can lower your footprint by up to 2 tons per year. Try a 'Meatless Monday'!"
            input.contains("car", ignoreCase = true) -> "Switching to public transport or a bike can significantly reduce transport emissions."
            input.contains("energy", ignoreCase = true) -> "LED bulbs and better insulation are great first steps for home energy efficiency."
            else -> "That's a great question! Small changes in our daily habits, like choosing local produce, make a big difference for the planet."
        }
    }
}

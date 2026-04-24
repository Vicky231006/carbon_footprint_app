package com.example.theglobalcarbonfootprintproject.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theglobalcarbonfootprintproject.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
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

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(ChatMessage(text, true))
        _uiState.value = _uiState.value.copy(messages = currentMessages, isLoading = true)

        viewModelScope.launch {
            try {
                val response = generativeModel.generateContent(text)
                val aiResponse = response.text ?: "I'm sorry, I couldn't process that. Please try again."
                
                val updatedMessages = _uiState.value.messages.toMutableList()
                updatedMessages.add(ChatMessage(aiResponse, false))
                _uiState.value = _uiState.value.copy(messages = updatedMessages, isLoading = false)
            } catch (e: Exception) {
                val updatedMessages = _uiState.value.messages.toMutableList()
                updatedMessages.add(ChatMessage("Error: ${e.message}", false))
                _uiState.value = _uiState.value.copy(messages = updatedMessages, isLoading = false)
            }
        }
    }
}

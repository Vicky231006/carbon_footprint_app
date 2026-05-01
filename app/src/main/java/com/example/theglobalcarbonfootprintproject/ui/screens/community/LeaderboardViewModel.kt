package com.example.theglobalcarbonfootprintproject.ui.screens.community

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


data class LeaderboardUiState(
    val users: List<LeaderboardEntry> = emptyList()
)

data class LeaderboardEntry(
    val username: String,
    val points: Int,
    val rank: Int
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: com.example.theglobalcarbonfootprintproject.data.repository.CarbonRepository,
    private val sharedPreferences: android.content.SharedPreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow("INDIVIDUAL")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    init {
        refreshLeaderboard()
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
        refreshLeaderboard()
    }

    fun refreshLeaderboard() {
        viewModelScope.launch {
            try {
                val state = repository.getUserProfile().first()?.state
                val entries = repository.getLeaderboard(state, _selectedCategory.value)
                
                val rankedEntries = entries.mapIndexed { index, entry ->
                    LeaderboardEntry(
                        username = entry.name,
                        points = entry.baseline_co2_daily.toInt(), // Using daily baseline as proxy for score
                        rank = index + 1
                    )
                }
                
                _uiState.value = LeaderboardUiState(rankedEntries)
            } catch (e: Exception) {
                Log.e("Leaderboard", "Error refreshing leaderboard", e)
            }
        }
    }
}


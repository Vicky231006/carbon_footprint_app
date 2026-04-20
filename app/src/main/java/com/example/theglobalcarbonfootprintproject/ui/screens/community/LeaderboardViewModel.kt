package com.example.theglobalcarbonfootprintproject.ui.screens.community

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class LeaderboardUiState(
    val users: List<LeaderboardEntry> = listOf(
        LeaderboardEntry("EcoWarrior_42", 12500, 1),
        LeaderboardEntry("GreenLeaf", 11200, 2),
        LeaderboardEntry("SolarPioneer", 9800, 3),
        LeaderboardEntry("CarbonNeutral", 8500, 4),
        LeaderboardEntry("You", 7200, 5)
    )
)

data class LeaderboardEntry(
    val username: String,
    val points: Int,
    val rank: Int
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState = _uiState.asStateFlow()
}

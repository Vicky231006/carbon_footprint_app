package com.example.theglobalcarbonfootprintproject.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theglobalcarbonfootprintproject.data.repository.CarbonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val repository: CarbonRepository
) : ViewModel() {
    
    val uiState: StateFlow<LeaderboardUiState> = repository.getTotalPoints()
        .map { points ->
            val userPoints = points ?: 0
            val mockUsers = listOf(
                LeaderboardEntry("EcoWarrior_42", 12500, 1),
                LeaderboardEntry("GreenLeaf", 11200, 2),
                LeaderboardEntry("SolarPioneer", 9800, 3),
                LeaderboardEntry("CarbonNeutral", 8500, 4),
                LeaderboardEntry("You", userPoints, 5)
            ).sortedByDescending { it.points }
            
            // Re-rank based on points
            val rankedUsers = mockUsers.mapIndexed { index, entry ->
                entry.copy(rank = index + 1)
            }
            
            LeaderboardUiState(rankedUsers)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LeaderboardUiState()
        )
}

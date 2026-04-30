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
                LeaderboardEntry("Aditya Kulkarni", userPoints + 1200, 1),
                LeaderboardEntry("Vicky (You)", userPoints, 2),
                LeaderboardEntry("Snehal Patil", (userPoints * 0.85).toInt(), 3),
                LeaderboardEntry("Rahul Deshmukh", (userPoints * 0.70).toInt(), 4),
                LeaderboardEntry("Anjali Joshi", (userPoints * 0.55).toInt(), 5)
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

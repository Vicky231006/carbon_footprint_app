package com.example.theglobalcarbonfootprintproject.ui.screens.community

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry
import com.example.theglobalcarbonfootprintproject.data.remote.MongoApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.theglobalcarbonfootprintproject.data.repository.CarbonRepository


data class CommunityUiState(
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val userState: String = "Global",
    val isInstitution: Boolean = false,
    val institutionName: String? = null
)



@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val apiService: MongoApiService,
    private val repository: CarbonRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {


    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchLeaderboard()
    }

    fun fetchLeaderboard() {
        val prefs = context.getSharedPreferences("carbon_prefs", Context.MODE_PRIVATE)
        val userType = prefs.getString("user_type", "INDIVIDUAL")
        val isInst = userType == "INSTITUTION"
        val userState = prefs.getString("user_state", "Maharashtra") ?: "Maharashtra"
        val instName = prefs.getString("institution_name", "Our Institution")

        _uiState.value = _uiState.value.copy(
            isLoading = true, 
            userState = userState,
            isInstitution = isInst,
            institutionName = instName
        )
        
        viewModelScope.launch {
            try {
                val response = apiService.getLeaderboard(userState)
                if (response.success && response.leaderboard != null && response.leaderboard.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        leaderboard = response.leaderboard,
                        isLoading = false,
                        error = null
                    )
                } else {
                    // Fallback to MOCK data for demo if API returns empty or fails
                    val mockLeaderboard = if (isInst) {
                        listOf(
                            com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                                "IIT Bombay", userState, 0.72
                            ),
                            com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                                "VJTI Mumbai", userState, 0.85
                            ),
                            com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                                instName ?: "CRCE Mumbai", userState, 0.94
                            ),
                            com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                                "NMIMS Mumbai", userState, 1.12
                            ),
                            com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                                "KJ Somaiya", userState, 1.25
                            )
                        )
                    } else {
                        listOf(
                            com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                                "Aditya Kulkarni", userState, 5.5
                            ),
                            com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                                "Vicky (You)", userState, 8.2
                            ),
                            com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                                "Snehal Patil", userState, 10.5
                            ),
                            com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                                "Rahul Deshmukh", userState, 12.8
                            ),
                            com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                                "Anjali Joshi", userState, 15.3
                            )
                        )
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        leaderboard = mockLeaderboard,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                // Same fallback for connection errors
                val mockLeaderboard = if (isInst) {
                    listOf(
                        com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                            "IIT Bombay", userState, 0.72
                        ),
                        com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                            "VJTI Mumbai", userState, 0.85
                        ),
                        com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                            instName ?: "CRCE Mumbai", userState, 0.94
                        ),
                        com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                            "NMIMS Mumbai", userState, 1.12
                        ),
                        com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                            "KJ Somaiya", userState, 1.25
                        )
                    )
                } else {
                    listOf(
                        com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                            "Aditya Kulkarni", userState, 5.5
                        ),
                        com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                            "Vicky (You)", userState, 8.2
                        ),
                        com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                            "Snehal Patil", userState, 10.5
                        ),
                        com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                            "Rahul Deshmukh", userState, 12.8
                        ),
                        com.example.theglobalcarbonfootprintproject.data.remote.LeaderboardEntry(
                            "Anjali Joshi", userState, 15.3
                        )
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    leaderboard = mockLeaderboard,
                    isLoading = false,
                    error = null
                )
            }

        }




    }
}


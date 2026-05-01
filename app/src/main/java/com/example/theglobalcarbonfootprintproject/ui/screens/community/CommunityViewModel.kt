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
                val response = apiService.getLeaderboard(userState, userType)

                if (response.success && response.leaderboard != null && response.leaderboard.isNotEmpty()) {
                    val localScore = prefs.getInt("current_carbon_score", -1)
                    val currentName = prefs.getString("user_name", "") ?: ""
                    
                    val updatedLeaderboard = response.leaderboard.map { entry ->
                        if (entry.name == currentName && localScore != -1) {
                            entry.copy(baseline_co2_daily = localScore.toDouble())
                        } else {
                            entry
                        }
                    }.sortedByDescending { it.baseline_co2_daily }

                    _uiState.value = _uiState.value.copy(
                        leaderboard = updatedLeaderboard,
                        isLoading = false,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        leaderboard = emptyList(),
                        isLoading = false,
                        error = "No peers found yet"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    leaderboard = emptyList(),
                    isLoading = false,
                    error = e.localizedMessage ?: "Failed to load leaderboard"
                )

            }

        }




    }
}


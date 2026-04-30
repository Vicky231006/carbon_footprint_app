package com.example.theglobalcarbonfootprintproject.ui.screens.auth

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theglobalcarbonfootprintproject.data.local.dao.CarbonDao
import com.example.theglobalcarbonfootprintproject.data.remote.LoginRequest
import com.example.theglobalcarbonfootprintproject.data.remote.MongoApiService
import com.example.theglobalcarbonfootprintproject.data.remote.RegisterRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val registeredUserId: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiService: MongoApiService,
    private val carbonDao: CarbonDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun login(email: String, pass: String, prefs: SharedPreferences) {
        _uiState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val response = apiService.loginUser(LoginRequest(email, pass))
                if (response.success && response.user != null) {
                    val userId = response.userId ?: "unknown"
                    
                    // Populate local database with retrieved user
                    carbonDao.insertUserProfile(response.user)
                    
                    // Handle Institution data if present
                    response.institution?.let { 
                        carbonDao.insertInstitutionProfile(it)
                    }

                    val userType = if (response.institution != null) "INSTITUTION" else response.user.userType

                    prefs.edit()
                        .putBoolean("onboarding_complete", response.user.onboardingComplete)
                        .putString("user_id", userId)
                        .putFloat("baseline_co2_daily", response.user.baseline_co2_daily.toFloat())
                        .putString("user_type", userType)
                        .putString("user_name", response.user.name)
                        .putString("user_state", response.user.state)
                        .putFloat("grid_factor", response.user.gridFactor.toFloat())
                        .apply()


                    // Trigger a pull of historical logs from MongoDB
                    viewModelScope.launch {
                        try {
                            val logsResponse = apiService.getLogs(userId)
                            if (logsResponse.success && logsResponse.logs != null) {
                                logsResponse.logs.forEach { log ->
                                    carbonDao.insertLog(log)
                                }
                            }
                        } catch (e: Exception) {
                            // Silent failure for logs pull
                        }
                    }
                        
                    _uiState.value = AuthUiState(isLoggedIn = true)

                } else {
                    _uiState.value = AuthUiState(error = response.error ?: "Invalid credentials")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.localizedMessage ?: "Connection failed")
            }
        }
    }

    fun register(email: String, pass: String, prefs: SharedPreferences) {
        _uiState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            try {
                // Pass empty strings for name and state as they will be updated in onboarding
                val response = apiService.registerUser(RegisterRequest(email, pass, "", ""))
                if (response.success && response.userId != null) {
                    prefs.edit()
                        .putString("user_id", response.userId)
                        .putString("user_email", email)
                        .apply()
                    _uiState.value = AuthUiState(registeredUserId = response.userId)
                } else {
                    _uiState.value = AuthUiState(error = response.error ?: "Registration failed")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.localizedMessage ?: "Connection failed")
            }
        }
    }

}

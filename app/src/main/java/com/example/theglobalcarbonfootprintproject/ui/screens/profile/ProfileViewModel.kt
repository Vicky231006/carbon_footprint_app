package com.example.theglobalcarbonfootprintproject.ui.screens.profile

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theglobalcarbonfootprintproject.data.repository.CarbonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: CarbonRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    val userName = prefs.getString("user_name", "User") ?: "User"
    val userState = prefs.getString("user_state", "Unknown") ?: "Unknown"
    val baselineCo2 = prefs.getFloat("baseline_co2_daily", 0f).toDouble()

    private val _avgCo2 = MutableStateFlow(0.0)
    val avgCo2 = _avgCo2.asStateFlow()

    private val _bestCo2 = MutableStateFlow(0.0)
    val bestCo2 = _bestCo2.asStateFlow()

    private val _daysCount = MutableStateFlow(0)
    val daysCount = _daysCount.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllLogs().collect { logs ->
                if (logs.isNotEmpty()) {
                    _avgCo2.value = logs.map { it.totalKg }.average()
                    _bestCo2.value = logs.minOf { it.totalKg }
                    _daysCount.value = logs.size
                }
            }
        }
    }

    fun signOut(context: Context, onComplete: () -> Unit) {
        prefs.edit().clear().apply()
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearUserProfile()
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun syncToMongo(onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userType = prefs.getString("user_type", "INDIVIDUAL")
                var success = false
                if (userType == "INDIVIDUAL") {
                    val profile = repository.getUserProfile().firstOrNull()
                    if (profile != null) {
                        repository.saveUserProfile(profile)
                        success = true
                    }
                } else {
                    val profile = repository.getInstitutionProfile().firstOrNull()
                    if (profile != null) {
                        repository.saveInstitutionProfile(profile)
                        success = true
                    }
                }
                withContext(Dispatchers.Main) { onResult(success) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }
}

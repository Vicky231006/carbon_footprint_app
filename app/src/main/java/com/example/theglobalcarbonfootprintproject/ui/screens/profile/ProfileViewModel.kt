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
}

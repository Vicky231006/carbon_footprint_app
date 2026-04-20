package com.example.theglobalcarbonfootprintproject.ui.screens.dashboard

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theglobalcarbonfootprintproject.data.local.entities.CarbonLog
import com.example.theglobalcarbonfootprintproject.data.local.entities.TransportSegment
import com.example.theglobalcarbonfootprintproject.data.repository.CarbonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: CarbonRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _todayLog = MutableStateFlow<CarbonLog?>(null)
    val todayLog: StateFlow<CarbonLog?> = _todayLog.asStateFlow()

    private val _carbonScore = MutableStateFlow(100)
    val carbonScore: StateFlow<Int> = _carbonScore.asStateFlow()

    val transportCo2Today: StateFlow<Double> = repository
        .getTodayTransportCo2(startOfDay())
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val foodCo2Today: StateFlow<Double> = repository
        .getTodayFoodCo2(startOfDay())
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val energyCo2Today: StateFlow<Double> = repository
        .getTodayEnergyCo2(startOfDay())
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalCo2Today: StateFlow<Double> = combine(
        transportCo2Today,
        foodCo2Today,
        energyCo2Today,
        _todayLog.map { it?.totalKg ?: 0.0 }
    ) { trans, food, energy, legacy ->
        trans + food + energy + legacy
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _stepsToday = MutableStateFlow(0)
    val stepsToday: StateFlow<Int> = _stepsToday.asStateFlow()

    val unverifiedSegments: StateFlow<List<TransportSegment>> = repository
        .getTodaySegments(startOfDay())
        .map { segments -> segments.filter { !it.userVerified && it.activityType == 0 } } // 0 is DetectedActivity.IN_VEHICLE
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val observer = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "steps_today") {
            updateSteps()
        }
    }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(observer)
        
        viewModelScope.launch {
            totalCo2Today.collect { total ->
                val dailyAvg = 4.1
                _carbonScore.value = (100 - (total / dailyAvg * 50)).toInt().coerceIn(0, 100)
            }
        }

        refreshDashboard()
        updateSteps()
    }

    override fun onCleared() {
        super.onCleared()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(observer)
    }

    private fun startOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun updateSteps() {
        _stepsToday.value = sharedPreferences.getInt("steps_today", 0)
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            repository.getAllLogs().collect { logs ->
                if (logs.isNotEmpty()) {
                    val latest = logs.last() 
                    _todayLog.value = latest

                    val dailyAvg = 4.1
                    _carbonScore.value = (100 - (latest.totalKg / dailyAvg * 50)).toInt().coerceIn(0, 100)
                }
            }
        }
    }

    fun verifySegment(segmentId: Int, mode: com.example.theglobalcarbonfootprintproject.calculator.TransportMode) {
        viewModelScope.launch {
            repository.updateTransportMode(segmentId, mode)
        }
    }
}

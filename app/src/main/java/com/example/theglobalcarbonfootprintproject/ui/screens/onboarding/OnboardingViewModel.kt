package com.example.theglobalcarbonfootprintproject.ui.screens.onboarding

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theglobalcarbonfootprintproject.calculator.CarbonEngine
import com.example.theglobalcarbonfootprintproject.data.local.dao.CarbonDao
import com.example.theglobalcarbonfootprintproject.data.local.entities.CarbonLog
import com.example.theglobalcarbonfootprintproject.data.local.entities.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val carbonDao: CarbonDao
) : ViewModel() {

    private val _data = MutableStateFlow(OnboardingData())
    val data: StateFlow<OnboardingData> = _data.asStateFlow()

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    val totalSteps: Int
        get() = if (_data.value.userType == UserType.INDIVIDUAL) 10 else 9

    val progressFraction: Float
        get() = if (totalSteps > 0) _currentStep.value.toFloat() / totalSteps else 0f

    fun updateData(block: (OnboardingData) -> OnboardingData) {
        _data.update { block(it) }
    }

    fun nextStep() {
        _currentStep.update { it + 1 }
    }

    fun prevStep() {
        _currentStep.update { (it - 1).coerceAtLeast(0) }
    }

    fun saveToPreferences(prefs: SharedPreferences) {
        val breakdown = CarbonEngine.calculateBreakdown(_data.value)
        val baseline = breakdown.values.sum()
        val currentData = _data.value

        viewModelScope.launch {
            val profile = UserProfile(
                name = currentData.name,
                userType = currentData.userType.name.lowercase(),
                dietType = currentData.dietType.name.lowercase(),
                travelMode = currentData.primaryMode.name.lowercase(),
                acUsage = currentData.acUsage != ACUsage.NONE,
                monthlyBill = currentData.monthlyBillRupees,
                screenTimeCategory = currentData.screenTimeCategory.name.lowercase()
            )
            carbonDao.insertUserProfile(profile)

            // Log initial baseline as first entry
            val initialLog = CarbonLog(
                date = System.currentTimeMillis(),
                transportKg = breakdown["Transport"] ?: 0.0,
                electricityKg = breakdown["Energy"] ?: 0.0,
                foodKg = breakdown["Food"] ?: 0.0,
                digitalKg = breakdown["Digital"] ?: 0.0,
                totalKg = baseline,
                greenPoints = 50 // Welcome points
            )
            carbonDao.insertLog(initialLog)

            prefs.edit()
                .putBoolean("onboarding_complete", true)
                .putFloat("baseline_co2_daily", baseline.toFloat())
                .putString("user_type", currentData.userType.name)
                .putString("user_name", currentData.name)
                .putFloat("grid_factor", currentData.gridFactor.toFloat())
                .putLong("onboarding_timestamp", System.currentTimeMillis())
                .apply()
        }
    }
}

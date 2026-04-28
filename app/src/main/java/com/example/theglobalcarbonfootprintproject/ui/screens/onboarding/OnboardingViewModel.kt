package com.example.theglobalcarbonfootprintproject.ui.screens.onboarding

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theglobalcarbonfootprintproject.calculator.CarbonEngine
import com.example.theglobalcarbonfootprintproject.calculator.InstitutionCarbonEngine
import com.example.theglobalcarbonfootprintproject.data.local.dao.CarbonDao
import com.example.theglobalcarbonfootprintproject.data.local.entities.CarbonLog
import com.example.theglobalcarbonfootprintproject.data.local.entities.InstitutionProfile
import com.example.theglobalcarbonfootprintproject.data.local.entities.UserProfile
import com.example.theglobalcarbonfootprintproject.data.repository.CarbonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.gson.Gson

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: CarbonRepository
) : ViewModel() {

    private val _data = MutableStateFlow(OnboardingData())
    val data: StateFlow<OnboardingData> = _data.asStateFlow()

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    val totalSteps: Int
        get() = 12

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

    fun saveToPreferences(prefs: SharedPreferences, onComplete: () -> Unit) {
        val currentData = _data.value
        val gson = Gson()
        
        viewModelScope.launch {
            val totalBaseline: Double
            if (currentData.userType == UserType.INDIVIDUAL) {
                val breakdown = CarbonEngine.calculateBreakdown(currentData)
                totalBaseline = breakdown.values.sum()
                
                val profile = UserProfile(
                    name = currentData.name,
                    userType = "INDIVIDUAL",
                    dietType = currentData.dietType.name,
                    travelMode = currentData.primaryMode.name,
                    fuelType = currentData.fuelType.name,
                    kmPerDay = currentData.kmPerDay,
                    mealsPerDay = currentData.mealsPerDay,
                    screenTimeCategory = currentData.screenTimeCategory.name,
                    deviceCount = currentData.deviceCount,
                    streamingHeavy = currentData.streamingHeavy,
                    acUsage = currentData.acUsage != ACUsage.NONE,
                    monthlyKwhBase = currentData.monthlyKwhBase,
                    acSeasonality = currentData.acSeasonality,
                    gridFactor = currentData.gridFactor
                )
                repository.saveUserProfile(profile)

                val initialLog = CarbonLog(
                    date = System.currentTimeMillis(),
                    transportKg = breakdown["Transport"] ?: 0.0,
                    electricityKg = breakdown["Energy"] ?: 0.0,
                    foodKg = breakdown["Food"] ?: 0.0,
                    digitalKg = breakdown["Digital"] ?: 0.0,
                    totalKg = totalBaseline,
                    greenPoints = 50
                )
                repository.saveLog(initialLog)
            } else {
                val studentCommuteSplitJson = gson.toJson(currentData.studentCommuteSplit.mapKeys { it.key.name })
                val annualEventsJson = gson.toJson(currentData.annualEvents)

                val instProfile = InstitutionProfile(
                    name = currentData.institutionName,
                    type = currentData.institutionType?.name ?: InstitutionType.OTHER.name,
                    city = currentData.city,
                    state = currentData.state,
                    gridFactor = currentData.gridFactor,
                    studentCount = currentData.studentCount,
                    staffCount = currentData.staffCount,
                    buildingFloors = currentData.buildingFloors,
                    classroomCount = currentData.classroomCount,
                    classroomAC = currentData.classroomAC.name,
                    labCount = currentData.labCount,
                    pcsPerLab = currentData.pcsPerLab,
                    labHoursPerDay = currentData.labHoursPerDay,
                    hasServerRoom = currentData.hasServerRoom,
                    serverRoomSize = currentData.serverRoomSize?.name,
                    monthlyEnergyKwh = currentData.monthlyEnergyKwh,
                    solarCapacityKw = currentData.solarCapacityKw,
                    generatorDieselLitresMonth = currentData.generatorDieselLitresMonth,
                    studentCommuteSplitJson = studentCommuteSplitJson,
                    avgCommuteKm = currentData.avgCommuteKm,
                    institutionBusCount = currentData.institutionBusCount,
                    busFuelType = currentData.busFuelType.name,
                    hasCanteen = currentData.hasCanteen,
                    canteenFuel = currentData.canteenFuel.name,
                    lpgCylindersMonth = currentData.lpgCylindersMonth,
                    dailyMealsServed = currentData.dailyMealsServed,
                    paperReavesMonth = currentData.paperReavesMonth,
                    annualEventsJson = annualEventsJson
                )
                repository.saveInstitutionProfile(instProfile)
                val instResult = InstitutionCarbonEngine.calculateDailyTotal(instProfile)
                totalBaseline = instResult.totalKg
                
                val initialLog = CarbonLog(
                    date = System.currentTimeMillis(),
                    transportKg = instResult.transportKg,
                    electricityKg = instResult.energyKg,
                    foodKg = instResult.foodKg,
                    digitalKg = 0.0, // Institutions don't track digital footprint like individuals
                    wasteKg = instResult.wasteKg,
                    eventKg = instResult.eventKg,
                    totalKg = totalBaseline,
                    greenPoints = 100
                )
                repository.saveLog(initialLog)
            }

            prefs.edit()
                .putBoolean("onboarding_complete", true)
                .putFloat("baseline_co2_daily", totalBaseline.toFloat())
                .putString("user_type", currentData.userType.name)
                .putString("user_name", if (currentData.userType == UserType.INDIVIDUAL) currentData.name else currentData.institutionName)
                .putString("user_state", currentData.state)
                .putFloat("grid_factor", currentData.gridFactor.toFloat())
                .putLong("onboarding_timestamp", System.currentTimeMillis())
                .apply()
                
            onComplete()
        }
    }
}

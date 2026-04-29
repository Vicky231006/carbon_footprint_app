package com.example.theglobalcarbonfootprintproject.ui.screens.dashboard

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope

import com.example.theglobalcarbonfootprintproject.calculator.CarbonEngine
import com.example.theglobalcarbonfootprintproject.calculator.InstitutionCarbonEngine
import com.example.theglobalcarbonfootprintproject.calculator.SeasonalElectricityCalculator
import com.example.theglobalcarbonfootprintproject.calculator.TransportMode as DomainTransportMode
import com.example.theglobalcarbonfootprintproject.data.local.Converters
import com.example.theglobalcarbonfootprintproject.data.local.entities.CarbonLog
import com.example.theglobalcarbonfootprintproject.data.local.entities.InstitutionProfile
import com.example.theglobalcarbonfootprintproject.data.local.entities.TransportSegment
import com.example.theglobalcarbonfootprintproject.data.remote.DailyLogRequest
import com.example.theglobalcarbonfootprintproject.data.remote.UserApiService
import com.example.theglobalcarbonfootprintproject.data.repository.CarbonRepository
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.DietType
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.FuelType
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.ScreenTime
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.TransportMode
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.UserType
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val repository: CarbonRepository,
    private val sharedPreferences: SharedPreferences,
    private val apiService: UserApiService,
    private val healthConnectManager: com.example.theglobalcarbonfootprintproject.data.health.HealthConnectManager
) : AndroidViewModel(application), DefaultLifecycleObserver {



    private val _todayLog = MutableStateFlow<CarbonLog?>(null)
    val todayLog: StateFlow<CarbonLog?> = _todayLog.asStateFlow()

    val isInstitution: StateFlow<Boolean> = sharedPreferences.asFlow()
        .map { it.getString("user_type", UserType.INDIVIDUAL.name) == UserType.INSTITUTION.name }
        .stateIn(viewModelScope, SharingStarted.Eagerly, sharedPreferences.getString("user_type", UserType.INDIVIDUAL.name) == UserType.INSTITUTION.name)

    val institutionProfile: StateFlow<InstitutionProfile?> = repository.getInstitutionProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val individualProfile = repository.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val transportCo2Today: StateFlow<Double> = combine(isInstitution, repository.getTodayTransportCo2(startOfDay()), institutionProfile, individualProfile) {
        isInst, trackedTransport, instProfile, indProfile ->
        if (isInst && instProfile != null) {
            val instResult = InstitutionCarbonEngine.calculateDailyTotal(instProfile)
            instResult.transportKg
        } else {
            // Logic for individuals: 
            // 1. If we have tracked data, use it.
            // 2. If tracked data is 0, use the profile baseline (from onboarding).
            // 3. This ensures that even if live tracking is catching up, the user sees an estimate.
            val tracked = trackedTransport ?: 0.0
            if (tracked > 0) {
                tracked
            } else if (indProfile != null) {
                // Baseline daily transport from profile
                CarbonEngine.calculateBreakdown(
                    com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.OnboardingData(
                        primaryMode = TransportMode.valueOf(indProfile.travelMode.uppercase()),
                        kmPerDay = indProfile.kmPerDay,
                        fuelType = FuelType.valueOf(indProfile.fuelType.uppercase()),
                        monthlyKwhBase = indProfile.monthlyKwhBase,
                        acSeasonality = indProfile.acSeasonality,
                        gridFactor = indProfile.gridFactor,
                        dietType = DietType.valueOf(indProfile.dietType.uppercase()),
                        mealsPerDay = indProfile.mealsPerDay,
                        screenTimeCategory = ScreenTime.valueOf(indProfile.screenTimeCategory.uppercase()),
                        deviceCount = indProfile.deviceCount,
                        streamingHeavy = indProfile.streamingHeavy
                    )
                )["Transport"] ?: 0.0
            } else 0.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val foodCo2Today: StateFlow<Double> = combine(isInstitution, repository.getTodayFoodCo2(startOfDay()), institutionProfile) {
        isInst, individualFood, instProfile ->
        if (isInst && instProfile != null) {
            val instResult = InstitutionCarbonEngine.calculateDailyTotal(instProfile)
            instResult.foodKg
        } else {
            individualFood ?: 0.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val energyCo2Today: StateFlow<Double> = combine(isInstitution, individualProfile, institutionProfile, repository.getTodayEnergyCo2(startOfDay())) {
            isInst, indProfile, instProfile, loggedEnergy ->
        if (isInst && instProfile != null) {
            val instResult = InstitutionCarbonEngine.calculateDailyTotal(instProfile)
            instResult.energyKg
        } else if (!isInst && indProfile != null) {
            val baseline = SeasonalElectricityCalculator.getDailyCo2(
                baseKwh = indProfile.monthlyKwhBase,
                seasonality = indProfile.acSeasonality,
                gridFactor = indProfile.gridFactor
            )
            baseline + (loggedEnergy ?: 0.0)
        } else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)


    val digitalCo2Today: StateFlow<Double> = combine(isInstitution, repository.getTodayDigitalCo2(startOfDay())) {
        isInst, individualDigital ->
        if (isInst) {
            0.0 // Institutions don't track digital footprint like individuals
        } else {
            individualDigital ?: 0.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val wasteCo2Today: StateFlow<Double> = combine(isInstitution, institutionProfile) {
        isInst, instProfile ->
        if (isInst && instProfile != null) {
            val instResult = InstitutionCarbonEngine.calculateDailyTotal(instProfile)
            instResult.wasteKg
        } else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val eventCo2Today: StateFlow<Double> = combine(isInstitution, institutionProfile) {
        isInst, instProfile ->
        if (isInst && instProfile != null) {
            val instResult = InstitutionCarbonEngine.calculateDailyTotal(instProfile)
            instResult.eventKg
        } else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val loggedEnergyToday: StateFlow<Double> = repository.getTodayEnergyCo2(startOfDay())
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)


    val totalCo2Today: StateFlow<Double> = combine(
        transportCo2Today,
        foodCo2Today,
        energyCo2Today,
        digitalCo2Today,
        wasteCo2Today,
        eventCo2Today,
        isInstitution
    ) { flows: Array<Any?> ->
        val trans = flows[0] as Double
        val food = flows[1] as Double
        val energy = flows[2] as Double
        val digital = flows[3] as Double
        val waste = flows[4] as Double
        val event = flows[5] as Double
        val isInst = flows[6] as Boolean

        if (isInst) {
            trans + food + energy + waste + event
        } else {
            trans + food + energy + digital
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)



    private val _carbonScore = MutableStateFlow(100)
    val carbonScore: StateFlow<Int> = _carbonScore.asStateFlow()

    val institutionName: StateFlow<String?> = institutionProfile.map { it?.name }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val transportKmToday: StateFlow<Double> = repository.getTodayMotorizedKm(startOfDay()).map { it ?: 0.0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val walkingKmToday: StateFlow<Double> = repository.getTodayWalkingKm(startOfDay()).map { it ?: 0.0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val studentStaffCount: StateFlow<Int> = institutionProfile.map { (it?.studentCount ?: 0) + (it?.staffCount ?: 0) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)


    private val _stepsToday = MutableStateFlow(0)
    val stepsToday: StateFlow<Int> = _stepsToday.asStateFlow()

    private val _healthConnectSteps = MutableStateFlow<Int?>(null)
    val healthConnectSteps: StateFlow<Int?> = _healthConnectSteps.asStateFlow()

    private val _healthPermissionsGranted = MutableStateFlow(false)

    val healthPermissionsGranted: StateFlow<Boolean> = _healthPermissionsGranted.asStateFlow()

    val isHealthConnectAvailable: Boolean
        get() = healthConnectManager.isAvailable()

    fun checkHealthPermissions() {
        viewModelScope.launch {
            _healthPermissionsGranted.value = healthConnectManager.hasAllPermissions()
        }
    }

    val unverifiedSegments: StateFlow<List<TransportSegment>> = repository

        .getTodaySegments(startOfDay())
        .map { segments -> segments.filter { !it.userVerified && it.activityType == 0 } } // 0 is DetectedActivity.IN_VEHICLE
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val observer = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "steps_today" || key == "user_type") {
            updateSteps()
            // Re-collect flows that depend on user_type
            viewModelScope.launch { isInstitution.collect() }
            viewModelScope.launch { institutionProfile.collect() }
            viewModelScope.launch { individualProfile.collect() }
        }
    }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(observer)
        
        viewModelScope.launch {
            repository.syncDigitalFootprint(getApplication())
            repository.syncCurrentUser()
        }

        viewModelScope.launch {
            totalCo2Today.collectLatest { total ->
                val currentIsInstitution = isInstitution.value
                if (currentIsInstitution) {
                    val totalPersons = studentStaffCount.value.toDouble()
                    val perPersonKg = if (totalPersons > 0) total / totalPersons else 0.0
                    _carbonScore.value = (4100 / (perPersonKg * 10)).toInt().coerceIn(0, 100)
                } else {
                    val dailyAvg = 9.58
                    _carbonScore.value = (100 - (total / dailyAvg * 50)).toInt().coerceIn(0, 100)
                    
                    // Reward logic: If total CO2 is significantly below average and not yet rewarded today
                    if (total > 0 && total < dailyAvg * 0.8) {
                        val start = startOfDay()
                        if (!repository.hasRewardForReason("efficiency_bonus", start)) {
                            repository.saveReward(
                                com.example.theglobalcarbonfootprintproject.data.local.entities.RewardRecord(
                                    date = System.currentTimeMillis(),
                                    pointsEarned = 20,
                                    reason = "efficiency_bonus"
                                )
                            )
                        }
                    }
                }
            }
        }

        refreshDashboard()
        checkHealthPermissions()
        updateSteps()
    }


    override fun onCleared() {
        super.onCleared()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(observer)
    }

    fun startOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    override fun onResume(owner: LifecycleOwner) {
        refreshDashboard()
    }

    fun updateSteps() {
        viewModelScope.launch {
            var fetchedSteps: Int? = null
            
            if (healthConnectManager.isAvailable() && healthConnectManager.hasAllPermissions()) {
                fetchedSteps = healthConnectManager.getStepsToday()?.toInt()
                Log.d("DashboardVM", "Health Connect steps: $fetchedSteps")
            }
            
            _healthConnectSteps.value = fetchedSteps
            val finalSteps = fetchedSteps ?: sharedPreferences.getInt("steps_today", 0)
            Log.d("DashboardVM", "Final steps displayed: $finalSteps")
            _stepsToday.value = finalSteps

            // Step bonus points
            val start = startOfDay()
            if (finalSteps >= 10000 && !repository.hasRewardForReason("steps_10k", start)) {
                repository.saveReward(com.example.theglobalcarbonfootprintproject.data.local.entities.RewardRecord(
                    date = System.currentTimeMillis(), pointsEarned = 20, reason = "steps_10k"
                ))
                Log.d("DashboardVM", "Awarded 20 pts for 10k steps")
            } else if (finalSteps >= 5000 && !repository.hasRewardForReason("steps_5k", start)) {
                repository.saveReward(com.example.theglobalcarbonfootprintproject.data.local.entities.RewardRecord(
                    date = System.currentTimeMillis(), pointsEarned = 10, reason = "steps_5k"
                ))
                Log.d("DashboardVM", "Awarded 10 pts for 5k steps")
            } else if (finalSteps >= 1000 && !repository.hasRewardForReason("steps_1k", start)) {
                repository.saveReward(com.example.theglobalcarbonfootprintproject.data.local.entities.RewardRecord(
                    date = System.currentTimeMillis(), pointsEarned = 5, reason = "steps_1k"
                ))
                Log.d("DashboardVM", "Awarded 5 pts for 1k steps")
            }
        }
    }





    fun refreshDashboard() {
        viewModelScope.launch {
            checkHealthPermissions()
            repository.syncDigitalFootprint(getApplication())
        }
        // Save today's snapshot as a CarbonLog (for history charts + MongoDB sync)
        // This is an Upsert: delete existing log for today and save fresh one
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Wait for profile to load (max 5 seconds)
                var count = 0
                while (individualProfile.value == null && count < 10) {
                    kotlinx.coroutines.delay(500)
                    count++
                }

                kotlinx.coroutines.delay(2000) // Extra time for flows to settle
                
                val trans = transportCo2Today.value
                val food = foodCo2Today.value
                val energy = energyCo2Today.value
                val digital = digitalCo2Today.value
                val waste = wasteCo2Today.value
                val event = eventCo2Today.value
                val total = trans + food + energy + digital + waste + event
                val points = _carbonScore.value

                if (total > 0) {
                    // Delete any previous log for today to avoid duplicates
                    repository.deleteLogsForDay(startOfDay())
                    
                    val log = CarbonLog(
                        date = System.currentTimeMillis(),
                        transportKg = trans,
                        electricityKg = energy,
                        digitalKg = digital,
                        foodKg = food,
                        wasteKg = waste,
                        eventKg = event,
                        totalKg = total,
                        greenPoints = points
                    )
                    repository.saveLog(log)
                    Log.d("DashboardVM", "Upserted daily log: total=$total kg")
                }
            } catch (e: Exception) {
                Log.w("DashboardVM", "Failed to save daily log: ${e.message}")
            }
        }
    }





    fun verifySegment(segmentId: Int, mode: DomainTransportMode) {
        viewModelScope.launch {
            repository.updateTransportMode(segmentId, mode)
        }
    }

    fun syncTodayToRemote() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val deviceId = "test_device_id" // TODO: Replace with actual deviceId
                val today    = LocalDate.now().toString()
                val isInst   = isInstitution.value

                // For individual user, we need the breakdown from CarbonEngine
                // For institution, we need the breakdown from InstitutionCarbonEngine
                // This part needs to be carefully constructed to provide the correct data for DailyLogRequest

                // Placeholder for now, will refine once repository provides aggregated data for sync
                // Placeholder for now, will refine once repository provides aggregated data for sync
                val currentCarbonScore = _carbonScore.value


                val logRequest = if (isInst) {
                    val instProfile = institutionProfile.first()
                    val instResult = instProfile?.let { InstitutionCarbonEngine.calculateDailyTotal(it) }
                    DailyLogRequest(
                        deviceId = deviceId,
                        date = today,
                        userType = UserType.INSTITUTION.name,
                        transportKg = instResult?.transportKg ?: 0.0,
                        energyKg = instResult?.energyKg ?: 0.0,
                        foodKg = instResult?.foodKg ?: 0.0,
                        digitalKg = 0.0,
                        wasteKg = instResult?.wasteKg ?: 0.0,
                        eventKg = instResult?.eventKg ?: 0.0,
                        totalKg = instResult?.totalKg ?: 0.0,
                        score = currentCarbonScore,
                        kmWalked = 0.0,
                        stepsCount = 0,
                        energyLogged = false,
                        foodLogged = false,
                        transportAutoDetected = false,
                        electricityMethod = "profile_estimate",
                        seasonLabel = ""
                    )
                } else {
                    val indProfile = individualProfile.first()
                    val indData = indProfile?.let { // Reconstruct OnboardingData from UserProfile
                        com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.OnboardingData(
                            name = it.name,
                            userType = UserType.INDIVIDUAL,
                            dietType = DietType.valueOf(it.dietType.uppercase()),
                            mealsPerDay = it.mealsPerDay,
                            primaryMode = TransportMode.valueOf(it.travelMode.uppercase()),
                            fuelType = FuelType.valueOf(it.fuelType.uppercase()),
                            kmPerDay = it.kmPerDay,
                            acSeasonality = it.acSeasonality,
                            monthlyKwhBase = it.monthlyKwhBase,
                            screenTimeCategory = ScreenTime.valueOf(it.screenTimeCategory.uppercase()),
                            deviceCount = it.deviceCount,
                            streamingHeavy = it.streamingHeavy,
                            gridFactor = it.gridFactor
                        )
                    }
                    val breakdown = indData?.let { CarbonEngine.calculateBreakdown(it) }
                    val electricityMethod = if (repository.hasLoggedEnergyToday(startOfDay()).first()) "logged" else "seasonal_estimate"
                    DailyLogRequest(
                        deviceId = deviceId,
                        date = today,
                        userType = UserType.INDIVIDUAL.name,
                        transportKg = breakdown?.get("Transport") ?: 0.0,
                        energyKg = breakdown?.get("Energy") ?: 0.0,
                        foodKg = breakdown?.get("Food") ?: 0.0,
                        digitalKg = breakdown?.get("Digital") ?: 0.0,
                        wasteKg = 0.0,
                        eventKg = 0.0,
                        totalKg = breakdown?.values?.sum() ?: 0.0,
                        score = currentCarbonScore,
                        kmWalked = repository.getTodayWalkingKm(startOfDay()).first() ?: 0.0,

                        stepsCount = sharedPreferences.getInt("steps_today", 0),
                        energyLogged = repository.hasLoggedEnergyToday(startOfDay()).first(),
                        foodLogged = repository.hasLoggedFoodToday(startOfDay()).first(),
                        transportAutoDetected = repository.getTodaySegments(startOfDay()).first().isNotEmpty(),
                        electricityMethod = electricityMethod,
                        seasonLabel = SeasonalElectricityCalculator.getSeasonLabel()
                    )
                }

                apiService.syncDailyLog(deviceId, logRequest)
            } catch (e: Exception) {
                Log.w("Sync", "Daily log sync failed: ${e.message}")
            }
        }
    }
}

private fun SharedPreferences.asFlow(): Flow<SharedPreferences> = callbackFlow {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, _ ->
        trySend(sharedPreferences)
    }
    registerOnSharedPreferenceChangeListener(listener)
    awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
}

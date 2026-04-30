package com.example.theglobalcarbonfootprintproject.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theglobalcarbonfootprintproject.data.repository.CarbonRepository
import com.example.theglobalcarbonfootprintproject.data.local.entities.CarbonLog
import com.example.theglobalcarbonfootprintproject.data.local.entities.FoodLog
import com.example.theglobalcarbonfootprintproject.data.local.entities.EnergyLog
import com.example.theglobalcarbonfootprintproject.data.local.entities.TransportSegment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.Calendar
import javax.inject.Inject

data class HistoryUiState(
    val transportHistory: List<TransportSegment> = emptyList(),
    val foodHistory: List<FoodLog> = emptyList(),
    val energyHistory: List<EnergyLog> = emptyList(),
    val digitalHistory: List<com.example.theglobalcarbonfootprintproject.data.local.entities.DigitalLog> = emptyList(),
    val dailyAggregates: List<CarbonLog> = emptyList(),
    val isInstitution: Boolean = false,
    val institutionProfile: com.example.theglobalcarbonfootprintproject.data.local.entities.InstitutionProfile? = null
)




data class DailyAggregate(
    val date: Long,
    val totalCo2: Double
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: CarbonRepository,
    private val sharedPreferences: android.content.SharedPreferences
) : ViewModel() {



    val uiState: StateFlow<HistoryUiState> = combine(
        repository.getAllTransportHistory(),
        repository.getAllFoodHistory(),
        repository.getAllEnergyHistory(),
        repository.getAllDigitalHistory(),
        repository.getAllLogs(),
        repository.getInstitutionProfile()
    ) { args ->
        val transport = args[0] as List<com.example.theglobalcarbonfootprintproject.data.local.entities.TransportSegment>
        val food = args[1] as List<com.example.theglobalcarbonfootprintproject.data.local.entities.FoodLog>
        val energy = args[2] as List<com.example.theglobalcarbonfootprintproject.data.local.entities.EnergyLog>
        val digital = args[3] as List<com.example.theglobalcarbonfootprintproject.data.local.entities.DigitalLog>
        val logs = args[4] as List<com.example.theglobalcarbonfootprintproject.data.local.entities.CarbonLog>
        val instProfile = args[5] as com.example.theglobalcarbonfootprintproject.data.local.entities.InstitutionProfile?

        val type = sharedPreferences.getString("user_type", "INDIVIDUAL")
        HistoryUiState(
            transportHistory = transport,
            foodHistory = food,
            energyHistory = energy,
            digitalHistory = digital,
            dailyAggregates = logs.sortedByDescending { it.date },
            isInstitution = type == "INSTITUTION",
            institutionProfile = instProfile
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )


}



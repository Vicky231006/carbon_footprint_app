package com.example.theglobalcarbonfootprintproject.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theglobalcarbonfootprintproject.data.repository.CarbonRepository
import com.example.theglobalcarbonfootprintproject.data.local.entities.FoodLog
import com.example.theglobalcarbonfootprintproject.data.local.entities.EnergyLog
import com.example.theglobalcarbonfootprintproject.data.local.entities.TransportSegment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class HistoryUiState(
    val transportHistory: List<TransportSegment> = emptyList(),
    val foodHistory: List<FoodLog> = emptyList(),
    val energyHistory: List<EnergyLog> = emptyList()
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: CarbonRepository
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.getAllTransportHistory(),
        repository.getAllFoodHistory(),
        repository.getAllEnergyHistory()
    ) { transport, food, energy ->
        HistoryUiState(transport, food, energy)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )
}

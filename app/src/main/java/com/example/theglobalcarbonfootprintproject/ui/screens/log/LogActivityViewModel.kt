package com.example.theglobalcarbonfootprintproject.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theglobalcarbonfootprintproject.data.local.entities.EnergyLog
import com.example.theglobalcarbonfootprintproject.data.local.entities.EnergyType
import com.example.theglobalcarbonfootprintproject.data.local.entities.FoodLog
import com.example.theglobalcarbonfootprintproject.data.local.entities.MealType
import com.example.theglobalcarbonfootprintproject.data.repository.CarbonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogActivityViewModel @Inject constructor(
    private val repository: CarbonRepository
) : ViewModel() {

    fun logFood(mealType: MealType, description: String) {
        viewModelScope.launch {
            val co2 = when (mealType) {
                MealType.HIGH_MEAT -> 2.5
                MealType.LOW_MEAT -> 1.5
                MealType.VEGETARIAN -> 0.8
                MealType.VEGAN -> 0.5
            }
            repository.saveFoodLog(
                FoodLog(
                    date = System.currentTimeMillis(),
                    mealType = mealType,
                    co2Kg = co2,
                    description = description
                )
            )
        }
    }

    fun logEnergy(energyType: EnergyType, value: Double) {
        viewModelScope.launch {
            val factor = when (energyType) {
                EnergyType.ELECTRICITY -> 0.85 // kg per kWh (approx India avg)
                EnergyType.NATURAL_GAS -> 2.0  // kg per unit
                EnergyType.LPG -> 2.98         // kg per kg
                EnergyType.COAL -> 2.42        // kg per kg
            }
            repository.saveEnergyLog(
                EnergyLog(
                    date = System.currentTimeMillis(),
                    energyType = energyType,
                    value = value,
                    co2Kg = value * factor
                )
            )
        }
    }
}

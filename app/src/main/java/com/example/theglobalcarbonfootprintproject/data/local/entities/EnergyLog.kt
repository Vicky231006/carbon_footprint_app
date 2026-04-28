package com.example.theglobalcarbonfootprintproject.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EnergyType { GRID, SOLAR, DIESEL, LPG, ELECTRICITY, NATURAL_GAS, COAL }

@Entity(tableName = "energy_logs")
data class EnergyLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val energyType: EnergyType = EnergyType.GRID,
    val value: Double = 0.0,
    val kwh: Double,
    val co2Kg: Double,
    val source: String = "GRID"
)

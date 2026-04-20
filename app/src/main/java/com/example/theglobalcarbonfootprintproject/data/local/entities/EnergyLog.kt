package com.example.theglobalcarbonfootprintproject.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "energy_logs")
data class EnergyLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val energyType: EnergyType,
    val value: Double, // kWh for electricity, kg for gas etc
    val co2Kg: Double
)

enum class EnergyType {
    ELECTRICITY,
    NATURAL_GAS,
    LPG,
    COAL
}

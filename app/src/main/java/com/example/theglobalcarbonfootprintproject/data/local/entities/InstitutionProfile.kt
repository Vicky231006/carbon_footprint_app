package com.example.theglobalcarbonfootprintproject.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "institution_profile")
data class InstitutionProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val type: String,
    val city: String,
    val state: String,
    val gridFactor: Double = 0.85,
    val studentCount: Int,
    val staffCount: Int,
    val buildingFloors: Int,
    val classroomCount: Int,
    val classroomAC: String,        // ClassroomAC name
    val labCount: Int,
    val pcsPerLab: Int,
    val labHoursPerDay: Double,
    val hasServerRoom: Boolean,
    val serverRoomSize: String?,    // ServerSize name
    val monthlyEnergyKwh: Double,
    val solarCapacityKw: Double,
    val generatorDieselLitresMonth: Double,
    val studentCommuteSplitJson: String,  // JSON Map<String,Int>
    val avgCommuteKm: Double,
    val institutionBusCount: Int,
    val busFuelType: String,        // FuelType name
    val hasCanteen: Boolean,
    val canteenFuel: String,        // CanteenFuel name
    val lpgCylindersMonth: Int,
    val dailyMealsServed: Int,
    val paperReavesMonth: Int,
    val annualEventsJson: String    // JSON List<AnnualEvent>
)

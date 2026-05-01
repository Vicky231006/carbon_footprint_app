package com.example.theglobalcarbonfootprintproject.ui.screens.onboarding

import com.example.theglobalcarbonfootprintproject.data.local.entities.ACSeasonality

data class OnboardingData(
    // Identity
    val userType: UserType = UserType.INDIVIDUAL,
    val name: String = "",
    val age: Int? = null,

    // Location
    val city: String = "",
    val state: String = "",
    val gridFactor: Double = 0.82,

    // Transport (Individual)
    val primaryMode: TransportMode = TransportMode.MIXED,
    val fuelType: FuelType = FuelType.PETROL,
    val kmPerDay: Double = 10.0,

    // Home energy (Individual)
    val acUsage: ACUsage = ACUsage.OCCASIONAL,
    val monthlyKwhBase: Double = 150.0,
    val acSeasonality: ACSeasonality = ACSeasonality.SEASONAL,
    val cookingType: CookingType = CookingType.LPG,
    val solarKwh: Double = 0.0,
    val hasSolarPanels: Boolean = false,

    // Diet (Individual)
    val dietType: DietType = DietType.MIXED,
    val mealsPerDay: Int = 3,
    val eatsOutFrequency: OutFrequency = OutFrequency.SOMETIMES,

    // Digital (Individual)
    val screenTimeCategory: ScreenTime = ScreenTime.MODERATE,
    val deviceCount: Int = 2,
    val streamingHeavy: Boolean = false,

    // Institution (if applicable)
    val institutionName: String = "",
    val institutionType: InstitutionType? = null,
    val studentCount: Int = 500,
    val staffCount: Int = 50,
    val buildingFloors: Int = 4,
    val classroomCount: Int = 16,
    val classroomAC: ClassroomAC = ClassroomAC.NONE,
    val labCount: Int = 5,
    val pcsPerLab: Int = 25,
    val labHoursPerDay: Double = 8.0,
    val hasServerRoom: Boolean = false,
    val serverRoomSize: ServerSize? = null,
    val monthlyEnergyKwh: Double = 10000.0,
    val solarCapacityKw: Double = 0.0,
    val generatorDieselLitresMonth: Double = 0.0,
    val studentCommuteSplit: Map<TransportMode, Int> = mapOf(
        TransportMode.BUS to 30,
        TransportMode.PUBLIC_TRANSPORT to 25,
        TransportMode.TWO_WHEELER to 20,
        TransportMode.CAR to 10,
        TransportMode.WALK_BIKE to 15
    ),
    val avgCommuteKm: Double = 10.0,
    val institutionBusCount: Int = 0,
    val busFuelType: FuelType = FuelType.DIESEL,
    val hasCanteen: Boolean = false,
    val canteenFuel: CanteenFuel = CanteenFuel.LPG,
    val lpgCylindersMonth: Int = 10,
    val dailyMealsServed: Int = 500,
    val paperReamsMonth: Int = 50,

    val annualEvents: List<AnnualEvent> = emptyList(),
    val departments: List<DepartmentBreakdown> = emptyList(),


    // Permissions result (saved after permissions screen)
    val activityPermissionGranted: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val usageAccessGranted: Boolean = false
)

enum class UserType { INDIVIDUAL, INSTITUTION }
enum class TransportMode { WALK_BIKE, METRO, BUS, TWO_WHEELER, CAR, MIXED, PUBLIC_TRANSPORT }
enum class FuelType { PETROL, DIESEL, ELECTRIC, CNG }
enum class ACUsage { NONE, OCCASIONAL, DAILY } // Individual AC usage for home
enum class CookingType { LPG, ELECTRIC, INDUCTION } // Individual cooking
enum class DietType { VEGAN, VEGETARIAN, MIXED, MEAT_HEAVY }
enum class OutFrequency { RARELY, SOMETIMES, OFTEN }
enum class ScreenTime { LIGHT, MODERATE, HEAVY }

// Institution Specific Enums
enum class InstitutionType { ENGINEERING_COLLEGE, ARTS_COLLEGE, SCHOOL, UNIVERSITY, OFFICE_CORPORATE, OTHER }
enum class ServerSize { SMALL, MEDIUM, LARGE }
enum class ClassroomAC { NONE, PARTIAL, FULL }
enum class CanteenFuel { LPG, PNG, ELECTRIC, MIXED }

data class AnnualEvent(
    val name: String,
    val attendance: Int,
    val durationDays: Int,
    val month: Int = 1 // 1-12
)

data class DepartmentBreakdown(
    val name: String,
    val studentCount: Int,
    val energyWeight: Double = 1.0 // Relative energy weight (e.g., Engineering Labs use more than Admin)
)


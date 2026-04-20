package com.example.theglobalcarbonfootprintproject.ui.screens.onboarding

data class OnboardingData(
    // Identity
    val userType: UserType = UserType.INDIVIDUAL,
    val name: String = "",
    val age: Int? = null,

    // Location
    val city: String = "",
    val state: String = "",
    val gridFactor: Double = 0.82,

    // Transport
    val primaryMode: TransportMode = TransportMode.MIXED,
    val fuelType: FuelType = FuelType.PETROL,
    val kmPerDay: Double = 10.0,

    // Home energy
    val acUsage: ACUsage = ACUsage.OCCASIONAL,
    val monthlyBillRupees: Double? = null,
    val cookingType: CookingType = CookingType.LPG,
    val solarKwh: Double = 0.0,
    val hasSolarPanels: Boolean = false,

    // Diet
    val dietType: DietType = DietType.MIXED,
    val mealsPerDay: Int = 3,
    val eatsOutFrequency: OutFrequency = OutFrequency.SOMETIMES,

    // Digital
    val screenTimeCategory: ScreenTime = ScreenTime.MODERATE,
    val deviceCount: Int = 2,
    val streamingHeavy: Boolean = false,

    // Institution (if applicable)
    val institutionName: String = "",
    val institutionType: InstitutionType? = null,
    val studentCount: Int = 0,
    val labCount: Int = 0,
    val acInLabs: Boolean = true,
    val labHoursPerDay: Double = 8.0,
    val hasServerRoom: Boolean = false,
    val serverRoomSize: ServerSize? = null,
    val fleetBusCount: Int = 0,
    val fleetFuelType: FuelType = FuelType.DIESEL,
    val fleetKmPerDay: Double = 50.0,

    // Permissions result (saved after permissions screen)
    val activityPermissionGranted: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val usageAccessGranted: Boolean = false
)

enum class UserType { INDIVIDUAL, INSTITUTION }
enum class TransportMode { WALK_BIKE, METRO, BUS, TWO_WHEELER, CAR, MIXED }
enum class FuelType { PETROL, DIESEL, ELECTRIC, CNG }
enum class ACUsage { NONE, OCCASIONAL, DAILY }
enum class CookingType { LPG, ELECTRIC, INDUCTION }
enum class DietType { VEGAN, VEGETARIAN, MIXED, MEAT_HEAVY }
enum class OutFrequency { RARELY, SOMETIMES, OFTEN }
enum class ScreenTime { LIGHT, MODERATE, HEAVY }
enum class InstitutionType { COLLEGE, SCHOOL, OFFICE, OTHER }
enum class ServerSize { SMALL, MEDIUM, LARGE }

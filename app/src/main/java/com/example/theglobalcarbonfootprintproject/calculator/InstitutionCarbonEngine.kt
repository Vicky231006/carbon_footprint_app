package com.example.theglobalcarbonfootprintproject.calculator

import com.example.theglobalcarbonfootprintproject.data.local.entities.InstitutionProfile
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.CanteenFuel
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.FuelType
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.TransportMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

object InstitutionCarbonEngine {

    fun calculateDailyTotal(data: InstitutionProfile): InstitutionResult {
        val energy    = calculateEnergyKg(data)
        val transport = calculateTransportKg(data)
        val food      = calculateFoodKg(data)
        val waste     = calculateWasteKg(data)
        val events    = calculateEventKg(data)
        return InstitutionResult(
            energyKg    = energy,
            transportKg = transport,
            foodKg      = food,
            wasteKg     = waste,
            eventKg     = events,
            totalKg     = energy + transport + food + waste + events
        )
    }

    fun calculateEnergyKg(d: InstitutionProfile): Double {
        // Classroom electricity (rooms × hours × kWh/room)
        val classroomKwh = d.classroomCount * 8.0 * 0.1 // 8 hours/day, 0.1 kWh/room average
        // AC labs (32 labs, hours/day, kWh per lab, PCs per lab) - approximated
        val labPcsKwh = d.labCount * d.pcsPerLab * d.labHoursPerDay * 0.15 // 0.15 kWh/PC
        val classroomAcEnum = com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.ClassroomAC.valueOf(d.classroomAC)
        val labAcKwh = if (classroomAcEnum == com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.ClassroomAC.FULL || classroomAcEnum == com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.ClassroomAC.PARTIAL) {
            d.labCount * d.labHoursPerDay * 2.0 // 2 kWh/lab for AC
        } else 0.0

        val serverFootprint = when (d.serverRoomSize?.uppercase()) {
            "SMALL" -> 50.0
            "MEDIUM" -> 150.0
            "LARGE" -> 500.0
            else -> 0.0
        }

        // 1. Monthly electricity → daily kWh
        val baseKwhDay = (d.monthlyEnergyKwh - d.solarCapacityKw * 4.5) / 30.0  // solar: avg 4.5h peak sun India
        // 2. Generator diesel (1 litre diesel = 2.68 kg CO₂)
        val generatorKg = (d.generatorDieselLitresMonth / 30.0) * 2.68
        // 3. Grid electricity CO₂
        val gridKg = (baseKwhDay + classroomKwh + labPcsKwh + labAcKwh + serverFootprint).coerceAtLeast(0.0) * d.gridFactor
        return gridKg + generatorKg
    }

    fun calculateTransportKg(d: InstitutionProfile): Double {
        val totalCommuters = d.studentCount + d.staffCount
        // For each mode, compute daily CO₂ contribution
        var total = 0.0

        val type = object : TypeToken<Map<String, Int>>() {}.type
        val studentCommuteSplit: Map<String, Int> = Gson().fromJson(d.studentCommuteSplitJson, type)

        val totalPct = studentCommuteSplit.values.sum().toFloat()

        studentCommuteSplit.forEach { (modeString, pct) ->
            val mode = TransportMode.valueOf(modeString.uppercase(Locale.getDefault()))
            val count  = totalCommuters * (pct / totalPct)
            val kmOneWay = d.avgCommuteKm
            val factor = TransportConstants.emissionFactor(mode)
            total += count * kmOneWay * 2 * factor  // × 2 for return
        }
        // Institution buses
        val busKg = d.institutionBusCount *
                TransportConstants.BUS_KM_PER_DAY *
                TransportConstants.emissionFactor(
                    if (d.busFuelType.uppercase(Locale.getDefault()) == FuelType.ELECTRIC.name)
                        TransportMode.METRO
                    else TransportMode.BUS
                )
        total += busKg
        return total
    }

    fun calculateFoodKg(d: InstitutionProfile): Double {
        if (!d.hasCanteen) return 0.0

        val canteenFuelType = CanteenFuel.valueOf(d.canteenFuel.uppercase(Locale.getDefault()))
        val lpgKg = if (canteenFuelType == CanteenFuel.LPG || canteenFuelType == CanteenFuel.MIXED) {
            // LPG: 1 cylinder = 14.2 kg LPG = 42.8 kg CO₂
            (d.lpgCylindersMonth / 30.0) * 42.8
        } else 0.0

        // Food waste: 1 kg food waste ≈ 2.5 kg CO₂ (landfill)
        // Estimate waste at 15% of meals × 0.4 kg avg meal weight
        val wasteKg = d.dailyMealsServed * 0.15 * 0.4 * 2.5
        return lpgKg + wasteKg
    }

    fun calculateWasteKg(d: InstitutionProfile): Double {
        // Paper: 1 ream (500 sheets A4) = ~2.1 kg CO₂
        val paperKg = (d.paperReavesMonth / 30.0) * 2.1
        return paperKg
    }

    fun calculateEventKg(d: InstitutionProfile): Double {
        val type = object : TypeToken<List<com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.AnnualEvent>>() {}.type
        val annualEvents: List<com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.AnnualEvent> = Gson().fromJson(d.annualEventsJson, type) ?: emptyList()

        if (annualEvents.isEmpty()) return 0.0
        val annualKg = annualEvents.sumOf { event ->
            event.attendance * event.durationDays * 2.5
        }
        return annualKg / 365.0  // spread to daily contribution
    }
}

data class InstitutionResult(
    val energyKg: Double,
    val transportKg: Double,
    val foodKg: Double,
    val wasteKg: Double,
    val eventKg: Double,
    val totalKg: Double
)

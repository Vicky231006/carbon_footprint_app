package com.example.theglobalcarbonfootprintproject.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.theglobalcarbonfootprintproject.data.local.entities.InstitutionProfile
import java.util.Locale

@Composable
fun InstitutionDashboardContent(
    profile: InstitutionProfile?,
    energyKg: Double,
    transportKg: Double,
    foodKg: Double,
    wasteKg: Double,
    eventKg: Double,
    studentStaffCount: Int
) {
    var expandedCategory by remember { mutableStateOf<String?>(null) }

    Column {
        Text(
            text = "Campus Resource Breakdown",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // ─── Energy ───
        DashboardCategoryRow("Campus Energy", energyKg, Icons.Default.Bolt,
            expanded = expandedCategory == "energy",
            onClick = { expandedCategory = if (expandedCategory == "energy") null else "energy" }
        )
        AnimatedVisibility(visible = expandedCategory == "energy") {
            profile?.let { p ->
                BreakdownCard(
                    title = "Energy Calculation",
                    lines = listOf(
                        "Grid Factor: ${p.gridFactor} kg CO₂/kWh",
                        "Monthly Bill: ${p.monthlyEnergyKwh} kWh",
                        "Solar Offset: -${p.solarCapacityKw * 4.5 * 30} kWh/mo (est.)",
                        "Diesel Generator: ${p.generatorDieselLitresMonth} L/mo",
                        "Labs: ${p.labCount} labs, ${p.pcsPerLab} PCs, ${p.labHoursPerDay} hrs/day",
                        "Classrooms: ${p.classroomCount} rooms (${p.classroomAC} AC)"
                    )
                )
            }
        }

        // ─── Transport ───
        DashboardCategoryRow("Commuting & Fleet", transportKg, Icons.Default.DirectionsBus,
            expanded = expandedCategory == "transport",
            onClick = { expandedCategory = if (expandedCategory == "transport") null else "transport" }
        )
        AnimatedVisibility(visible = expandedCategory == "transport") {
            profile?.let { p ->
                BreakdownCard(
                    title = "Transport Calculation",
                    lines = listOf(
                        "Total Members: ${p.studentCount + p.staffCount}",
                        "Avg Commute: ${p.avgCommuteKm} km (one-way)",
                        "Institution Buses: ${p.institutionBusCount} (${p.busFuelType})",
                        "Commute Modes: Based on student split %",
                        "Note: Calculations account for two-way trips."
                    )
                )
            }
        }

        // ─── Food ───
        DashboardCategoryRow("Canteen & Food", foodKg, Icons.Default.Restaurant,
            expanded = expandedCategory == "food",
            onClick = { expandedCategory = if (expandedCategory == "food") null else "food" }
        )
        AnimatedVisibility(visible = expandedCategory == "food") {
            profile?.let { p ->
                BreakdownCard(
                    title = "Canteen Calculation",
                    lines = if (p.hasCanteen) {
                        listOf(
                            "Cooking Fuel: ${p.canteenFuel}",
                            "LPG Consumption: ${p.lpgCylindersMonth} cylinders/mo",
                            "Daily Meals: ${p.dailyMealsServed}",
                            "Food Waste: Est. at 15% of served meals",
                            "Emission: Fuel + Food Waste decay (2.5 kg CO₂/kg)"
                        )
                    } else listOf("No canteen registered for this institution.")
                )
            }
        }

        // ─── Waste ───
        DashboardCategoryRow("Paper & Waste", wasteKg, Icons.Default.DeleteSweep,
            expanded = expandedCategory == "waste",
            onClick = { expandedCategory = if (expandedCategory == "waste") null else "waste" }
        )
        AnimatedVisibility(visible = expandedCategory == "waste") {
            profile?.let { p ->
                BreakdownCard(
                    title = "Waste Calculation",
                    lines = listOf(
                        "Paper Usage: ${p.paperReavesMonth} reams/mo",
                        "Paper Footprint: 2.1 kg CO₂ per ream",
                        "Daily Avg: ${(p.paperReavesMonth / 30.0) * 2.1} kg CO₂"
                    )
                )
            }
        }

        // ─── Events ───
        if (eventKg > 0) {
            DashboardCategoryRow("Annual Events (Daily)", eventKg, Icons.Default.Celebration,
                expanded = expandedCategory == "events",
                onClick = { expandedCategory = if (expandedCategory == "events") null else "events" }
            )
            AnimatedVisibility(visible = expandedCategory == "events") {
                profile?.let { p ->
                    BreakdownCard(
                        title = "Event Amortization",
                        lines = listOf(
                            "Total annual event footprint amortized daily",
                            "Attendance & Duration factored per event",
                            "Avg Factor: 2.5 kg CO₂ per person-day"
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Institution Stats",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF1B5E20)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Members:", style = MaterialTheme.typography.bodyMedium)
                    Text("$studentStaffCount", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
                val total = energyKg + transportKg + foodKg + wasteKg + eventKg
                val perMember = if (studentStaffCount > 0) total / studentStaffCount else 0.0
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Per Member Daily:", style = MaterialTheme.typography.bodyMedium)
                    Text("%.2f kg".format(perMember), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        }
    }
}

